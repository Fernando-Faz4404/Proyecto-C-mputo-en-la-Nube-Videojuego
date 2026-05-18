package main;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import json.JSON_GameMessage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class PlayerHandler implements Runnable {

    public static final CopyOnWriteArrayList<PlayerHandler> playerHandlers = new CopyOnWriteArrayList<>();
    public static final GameLobby lobby = new GameLobby();

    private static final AtomicInteger numPlayers  = new AtomicInteger(1);
    private static final AtomicInteger redScore    = new AtomicInteger(0);
    private static final AtomicInteger blueScore   = new AtomicInteger(0);
    private static final AtomicInteger greenScore  = new AtomicInteger(0);
    private static final AtomicInteger yellowScore = new AtomicInteger(0);
    private static volatile long gameSeed = 0;

    private final int     internalId;
    private final Gson    gson = new Gson();
    private java.net.Socket socket;
    private DataOutputStream dos;
    private DataInputStream  dis;

    private String  playerId;
    private String  team;
    private double  posX, posY, angle;
    private int     health = 100;
    private boolean alive  = true;

    public PlayerHandler(java.net.Socket socket) {
        this.socket     = socket;
        this.internalId = numPlayers.getAndIncrement();
        try {
            this.dos = new DataOutputStream(socket.getOutputStream());
            this.dis = new DataInputStream(socket.getInputStream());

            // Read JOIN message
            String joinJson = dis.readUTF();
            JsonObject jo   = JsonParser.parseString(joinJson).getAsJsonObject();
            this.playerId   = jo.has("playerId") ? jo.get("playerId").getAsString()
                                                 : "Player" + internalId;
            int prefTeams   = jo.has("teamCount") ? jo.get("teamCount").getAsInt() : 2;

            String assignedTeam = lobby.addPlayer(playerId, prefTeams);
            if (assignedTeam == null) {
                // Lobby full — reject
                writeUTF("{\"type\":\"DISCONNECT\",\"playerId\":\"" + playerId + "\"}");
                closeQuietly();
                return;
            }
            this.team = assignedTeam;

            playerHandlers.add(this);
            System.out.println("[Lobby] " + playerId + " joined as " + team
                    + " (" + lobby.getCurrentPlayers() + "/" + lobby.getMaxPlayers() + ")");

            // Send current lobby state to the new player
            sendTo(this, gson.toJson(buildLobbyState()));

            // Notify others of updated lobby
            broadcastAll(gson.toJson(buildLobbyState()));

            // Auto-start when minimum players are met
            if (lobby.isReadyToStart()) {
                launchGame();
            }

        } catch (IOException e) {
            closeQuietly();
        }
    }

    @Override
    public void run() {
        while (socket != null && socket.isConnected()) {
            try {
                String msg = dis.readUTF();
                if (msg != null) handleMessage(msg);
            } catch (IOException e) {
                break;
            }
        }
        disconnect();
    }

    // ---- Message handling ----

    private void handleMessage(String json) {
        try {
            JsonObject jo   = JsonParser.parseString(json).getAsJsonObject();
            String     type = jo.get("type").getAsString();

            switch (type) {
                case "MOVE" -> {
                    JSON_GameMessage d = gson.fromJson(json, JSON_GameMessage.class);
                    if (d.x      != null) posX   = d.x;
                    if (d.y      != null) posY   = d.y;
                    if (d.angle  != null) angle  = d.angle;
                    if (d.health != null) health = d.health;
                    if (d.alive  != null) alive  = d.alive;
                    broadcastOthers(json);
                }
                case "SHOOT"  -> broadcastOthers(json);
                case "DEATH"  -> {
                    alive  = false;
                    health = 0;
                    incrementOpponentScore();
                    broadcastOthers(json);
                    checkRoundEnd();
                }
                case "POWERUP_COLLECTED" -> broadcastOthers(json);
                default -> {}
            }
        } catch (Exception e) {
            System.err.println("handleMessage error: " + e.getMessage());
        }
    }

    private static synchronized void checkRoundEnd() {
        if (lobby.getState() != GameLobby.State.IN_GAME) return;

        // Build a map of team → hasAlivePlayer for all active teams
        String[] allTeams = { "RED", "BLUE", "GREEN", "YELLOW" };
        Map<String, Boolean> teamAlive = new LinkedHashMap<>();
        for (int i = 0; i < lobby.getTeamCount(); i++) teamAlive.put(allTeams[i], false);
        for (PlayerHandler ph : playerHandlers) {
            if (teamAlive.containsKey(ph.team) && ph.alive)
                teamAlive.put(ph.team, true);
        }

        List<String> survivors = teamAlive.entrySet().stream()
                .filter(Map.Entry::getValue)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (survivors.size() > 1) return; // round still ongoing

        String winner = survivors.isEmpty() ? "DRAW" : survivors.get(0);
        boolean moreRounds = lobby.advanceRound(winner);
        int[]   wins       = lobby.getRoundWins();
        int     doneRound  = lobby.getCurrentRound() - 1;

        String endJson = new Gson().toJson(JSON_GameMessage.roundEnd(
                doneRound, GameLobby.TOTAL_ROUNDS, winner,
                wins[0], wins[1], wins[2], wins[3]));
        broadcastAll(endJson);
        System.out.println("[Server] Round " + doneRound + " ended. Winner: " + winner);

        if (moreRounds) {
            // Reset alive/health for next round
            for (PlayerHandler ph : playerHandlers) { ph.alive = true; ph.health = 100; }
            redScore.set(0); blueScore.set(0); greenScore.set(0); yellowScore.set(0);

            final long   newSeed   = System.currentTimeMillis();
            final int    nextRound = lobby.getCurrentRound();
            final String nextMap   = lobby.getCurrentMapResource();
            final int[]  finalWins = wins;

            new Thread(() -> {
                try { Thread.sleep(5000); } catch (InterruptedException ignored) {}
                String startJson = new Gson().toJson(JSON_GameMessage.roundStart(
                        nextRound, GameLobby.TOTAL_ROUNDS, nextMap, newSeed,
                        finalWins[0], finalWins[1], finalWins[2], finalWins[3]));
                broadcastAll(startJson);
                System.out.println("[Server] Round " + nextRound + " started. Map: " + nextMap);
            }, "RoundDelay-" + nextRound).start();
        } else {
            System.out.println("[Server] Game over. Round wins — RED:" + wins[0]
                    + " BLUE:" + wins[1] + " GREEN:" + wins[2] + " YELLOW:" + wins[3]);
        }
    }

    private void incrementOpponentScore() {
        int r = redScore.get(), b = blueScore.get(),
            g = greenScore.get(), y = yellowScore.get();
        switch (team) {
            case "RED"    -> b = blueScore.incrementAndGet();
            case "BLUE"   -> r = redScore.incrementAndGet();
            case "GREEN"  -> y = yellowScore.incrementAndGet();
            case "YELLOW" -> g = greenScore.incrementAndGet();
        }
        String scoreJson = gson.toJson(
                JSON_GameMessage.scoreUpdate(r, b, g, y));
        broadcastAll(scoreJson);
    }

    // ---- Lobby helpers ----

    private static synchronized void launchGame() {
        if (lobby.getState() == GameLobby.State.IN_GAME) return;
        lobby.startGame();
        gameSeed = System.currentTimeMillis();

        List<JSON_GameMessage.LobbyPlayer> lp = lobby.getEntries().stream()
                .map(e -> new JSON_GameMessage.LobbyPlayer(e.playerId, e.team))
                .collect(Collectors.toList());

        String startJson = new Gson().toJson(
                JSON_GameMessage.gameStart(lobby.getTeamCount(),
                        lobby.getMapResource(), gameSeed, lp));
        broadcastAll(startJson);
        System.out.println("[Server] Game started! seed=" + gameSeed);
    }

    private static JSON_GameMessage buildLobbyState() {
        List<JSON_GameMessage.LobbyPlayer> lp = lobby.getEntries().stream()
                .map(e -> new JSON_GameMessage.LobbyPlayer(e.playerId, e.team))
                .collect(Collectors.toList());
        String status = lobby.isReadyToStart() ? "STARTING" : "WAITING";
        return JSON_GameMessage.lobbyState(lobby.getTeamCount(), status,
                lobby.getMinPlayers(), lp);
    }

    // ---- Network helpers ----

    private static void broadcastAll(String msg) {
        for (PlayerHandler ph : playerHandlers) sendTo(ph, msg);
    }

    private void broadcastOthers(String msg) {
        for (PlayerHandler ph : playerHandlers) {
            if (ph.internalId != this.internalId) sendTo(ph, msg);
        }
    }

    private static void sendTo(PlayerHandler ph, String msg) {
        try {
            ph.dos.writeUTF(msg);
            ph.dos.flush();
        } catch (IOException e) {
            ph.disconnect();
        }
    }

    private void writeUTF(String msg) throws IOException {
        dos.writeUTF(msg);
        dos.flush();
    }

    private void disconnect() {
        playerHandlers.remove(this);
        lobby.removePlayer(playerId);
        System.out.println("[Lobby] " + playerId + " disconnected.");

        if (playerId != null) {
            broadcastAll(gson.toJson(JSON_GameMessage.disconnect(playerId)));
            broadcastAll(gson.toJson(buildLobbyState()));
        }
        closeQuietly();
    }

    private void closeQuietly() {
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
        try { if (dis    != null) dis.close();    } catch (IOException ignored) {}
        try { if (dos    != null) dos.close();    } catch (IOException ignored) {}
    }
}
