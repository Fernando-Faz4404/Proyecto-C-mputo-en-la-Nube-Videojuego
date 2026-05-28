package main;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import json.JSON_GameMessage;
import org.java_websocket.WebSocket;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Maneja la lógica de cada jugador conectado vía WebSocket.
 * Se eliminó Runnable/run() y el I/O TCP raw (Socket, DataInputStream, DataOutputStream).
 * Toda la E/S se hace a través de {@link WebSocket#send(String)}.
 */
public class PlayerHandler {

    // ---- Estado estático compartido ----

    public static final CopyOnWriteArrayList<PlayerHandler> playerHandlers = new CopyOnWriteArrayList<>();
    public static final GameLobby lobby = new GameLobby();

    /** Mapa conexión WebSocket → handler del jugador. */
    public static final ConcurrentHashMap<WebSocket, PlayerHandler> connMap = new ConcurrentHashMap<>();

    private static final AtomicInteger numPlayers = new AtomicInteger(1);
    private static final AtomicInteger redScore   = new AtomicInteger(0);
    private static final AtomicInteger blueScore  = new AtomicInteger(0);
    private static final AtomicInteger greenScore = new AtomicInteger(0);
    private static final AtomicInteger yellowScore= new AtomicInteger(0);

    private static volatile long gameSeed = 0;
    private static final int START_DELAY_SECONDS = 10;
    private static volatile long startDeadlineMs = 0;
    private static volatile Thread startCountdownThread;

    // Seguimiento de recogida de power-ups (autoridad en el servidor)
    private static final java.util.Set<Integer> collectedThisBatch =
            java.util.Collections.synchronizedSet(new java.util.HashSet<>());
    private static final AtomicInteger respawnBatchCount = new AtomicInteger(0);

    // ---- Estado por jugador ----

    private final int internalId;
    private final Gson gson = new Gson();
    private final org.java_websocket.WebSocket conn;

    private String playerId;
    private String team;
    private double posX, posY, angle;
    private int health = 100;
    private boolean alive = true;

    // ---- Constructor ----

    /**
     * Solo guarda la conexión e id interno. No realiza I/O.
     * La inicialización real del jugador ocurre en {@link #onMessage}.
     */
    public PlayerHandler(WebSocket conn) {
        this.conn = conn;
        this.internalId = numPlayers.getAndIncrement();
    }

    // ---- Puntos de entrada estáticos (llamados desde WsServer) ----

    /**
     * Llamado por WsServer cuando llega un mensaje de texto.
     * Si la conexión no está en connMap aún, procesa el JOIN y crea el handler.
     * Si ya existe, delega a handleMessage().
     */
    public static void onMessage(WebSocket conn, String json) {
        PlayerHandler ph = connMap.get(conn);

        if (ph == null) {
            // Primera vez: debe ser mensaje JOIN
            PlayerHandler newPh = new PlayerHandler(conn);
            try {
                JsonObject jo = JsonParser.parseString(json).getAsJsonObject();
                newPh.playerId = jo.has("playerId")
                        ? jo.get("playerId").getAsString()
                        : "Player" + newPh.internalId;
                int prefTeams = jo.has("teamCount") ? jo.get("teamCount").getAsInt() : 2;

                String assignedTeam = lobby.addPlayer(newPh.playerId, prefTeams);
                if (assignedTeam == null) {
                    // Lobby lleno — rechazar
                    conn.send("{\"type\":\"DISCONNECT\",\"playerId\":\"" + newPh.playerId + "\"}");
                    conn.close();
                    return;
                }
                newPh.team = assignedTeam;

                connMap.put(conn, newPh);
                playerHandlers.add(newPh);
                System.out.println("[Lobby] " + newPh.playerId + " joined as " + newPh.team
                        + " (" + lobby.getCurrentPlayers() + "/" + lobby.getMaxPlayers() + ")");

                // Enviar estado actual del lobby al nuevo jugador
                sendTo(newPh, new Gson().toJson(buildLobbyState()));

                // Notificar a los demás del lobby actualizado
                broadcastAll(new Gson().toJson(buildLobbyState()));

                // Auto-start cuando se alcanza el mínimo por equipo
                evaluateAutoStart();

            } catch (Exception e) {
                System.err.println("[PlayerHandler] Error en JOIN: " + e.getMessage());
                conn.close();
            }

        } else {
            // Jugador ya registrado: procesar mensaje normal
            ph.handleMessage(json);
        }
    }

    /**
     * Llamado por WsServer cuando una conexión se cierra.
     */
    public static void onClose(WebSocket conn) {
        PlayerHandler ph = connMap.remove(conn);
        if (ph != null) {
            ph.disconnect();
        }
    }

    // ---- Manejo de mensajes ----

    public void handleMessage(String json) {
        try {
            JsonObject jo = JsonParser.parseString(json).getAsJsonObject();
            String type = jo.get("type").getAsString();

            switch (type) {
                case "MOVE": {
                    JSON_GameMessage d = gson.fromJson(json, JSON_GameMessage.class);
                    if (d.x != null) posX = d.x;
                    if (d.y != null) posY = d.y;
                    if (d.angle != null) angle = d.angle;
                    if (d.health != null) health = d.health;
                    if (d.alive != null) alive = d.alive;
                    broadcastOthers(json);
                    break;
                }
                case "SHOOT":
                    broadcastOthers(json);
                    break;
                case "DEATH": {
                    alive = false;
                    health = 0;
                    incrementOpponentScore();
                    broadcastOthers(json);
                    checkRoundEnd();
                    break;
                }
                case "POWERUP_COLLECTED": {
                    broadcastOthers(json);
                    if (jo.has("powerUpIndex")) {
                        int idx = jo.get("powerUpIndex").getAsInt();
                        boolean isNew = collectedThisBatch.add(idx);
                        if (isNew && collectedThisBatch.size() >= 5) {
                            collectedThisBatch.clear();
                            int batch = respawnBatchCount.incrementAndGet();
                            String respawnJson = new Gson().toJson(
                                    JSON_GameMessage.powerUpRespawn(batch));
                            broadcastAll(respawnJson);
                            System.out.println("[Server] PowerUp respawn batch " + batch);
                        }
                    }
                    break;
                }
                default:
                    break;
            }
        } catch (Exception e) {
            System.err.println("handleMessage error: " + e.getMessage());
        }
    }

    private static synchronized void checkRoundEnd() {
        if (lobby.getState() != GameLobby.State.IN_GAME) return;

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

        if (survivors.size() > 1) return;

        String winner = survivors.isEmpty() ? "DRAW" : survivors.get(0);
        boolean moreRounds = lobby.advanceRound(winner);
        int[] wins = lobby.getRoundWins();
        int doneRound = lobby.getCurrentRound() - 1;

        String endJson = new Gson().toJson(JSON_GameMessage.roundEnd(
                doneRound, GameLobby.TOTAL_ROUNDS, winner,
                wins[0], wins[1], wins[2], wins[3]));
        broadcastAll(endJson);
        System.out.println("[Server] Round " + doneRound + " ended. Winner: " + winner);

        if (moreRounds) {
            for (PlayerHandler ph : playerHandlers) { ph.alive = true; ph.health = 100; }
            redScore.set(0); blueScore.set(0); greenScore.set(0); yellowScore.set(0);
            collectedThisBatch.clear();
            respawnBatchCount.set(0);

            final long newSeed = System.currentTimeMillis();
            final int nextRound = lobby.getCurrentRound();
            final String nextMap = lobby.getCurrentMapResource();
            final int[] finalWins = wins;

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
            forceDisconnectAll();
        }
    }

    private void incrementOpponentScore() {
        int r = redScore.get(), b = blueScore.get(),
            g = greenScore.get(), y = yellowScore.get();
        switch (team) {
            case "RED":    b = blueScore.incrementAndGet();   break;
            case "BLUE":   r = redScore.incrementAndGet();    break;
            case "GREEN":  y = yellowScore.incrementAndGet(); break;
            case "YELLOW": g = greenScore.incrementAndGet();  break;
        }
        String scoreJson = gson.toJson(JSON_GameMessage.scoreUpdate(r, b, g, y));
        broadcastAll(scoreJson);
    }

    // ---- Helpers de lobby ----

    private static synchronized void launchGame() {
        if (lobby.getState() == GameLobby.State.IN_GAME) return;
        cancelStartCountdownLocked();
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
        Integer countdown = getCountdownSeconds();
        String status = countdown != null ? "STARTING" : "WAITING";
        return JSON_GameMessage.lobbyState(lobby.getTeamCount(), status,
                lobby.getMinPlayers(), countdown, lp);
    }

    private static Integer getCountdownSeconds() {
        long deadline = startDeadlineMs;
        if (deadline <= 0 || lobby.getState() == GameLobby.State.IN_GAME || !lobby.isReadyToStart()) {
            return null;
        }
        long now = System.currentTimeMillis();
        long remainingMs = Math.max(0, deadline - now);
        return (int) ((remainingMs + 999) / 1000);
    }

    private static synchronized void evaluateAutoStart() {
        if (lobby.getState() == GameLobby.State.IN_GAME) {
            cancelStartCountdownLocked();
            return;
        }
        if (!lobby.isReadyToStart()) {
            cancelStartCountdownLocked();
            broadcastAll(new Gson().toJson(buildLobbyState()));
            return;
        }
        if (startDeadlineMs == 0) {
            startDeadlineMs = System.currentTimeMillis() + (START_DELAY_SECONDS * 1000L);
            startCountdownThread = new Thread(PlayerHandler::runStartCountdown, "LobbyStartCountdown");
            startCountdownThread.setDaemon(true);
            startCountdownThread.start();
            System.out.println("[Lobby] Minimum reached. Starting in " + START_DELAY_SECONDS + "s...");
        }
        broadcastAll(new Gson().toJson(buildLobbyState()));
    }

    private static void runStartCountdown() {
        while (true) {
            synchronized (PlayerHandler.class) {
                if (startDeadlineMs == 0
                        || lobby.getState() == GameLobby.State.IN_GAME
                        || !lobby.isReadyToStart()) {
                    startCountdownThread = null;
                    return;
                }
                if (System.currentTimeMillis() >= startDeadlineMs) break;
            }
            broadcastAll(new Gson().toJson(buildLobbyState()));
            try { Thread.sleep(1000); } catch (InterruptedException ignored) { return; }
        }

        synchronized (PlayerHandler.class) {
            startCountdownThread = null;
            if (startDeadlineMs == 0 || !lobby.isReadyToStart()
                    || lobby.getState() == GameLobby.State.IN_GAME) {
                return;
            }
            startDeadlineMs = 0;
        }
        launchGame();
    }

    private static void cancelStartCountdownLocked() {
        startDeadlineMs = 0;
        Thread t = startCountdownThread;
        startCountdownThread = null;
        if (t != null && t != Thread.currentThread()) t.interrupt();
    }

    // ---- Helpers de red ----

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
            ph.conn.send(msg);
        } catch (Exception e) {
            ph.disconnect();
        }
    }

    private void writeUTF(String msg) {
        try {
            conn.send(msg);
        } catch (Exception e) {
            System.err.println("writeUTF error: " + e.getMessage());
        }
    }

    private static void forceDisconnectAll() {
        List<PlayerHandler> snapshot = new ArrayList<>(playerHandlers);
        playerHandlers.clear();
        connMap.clear();
        for (PlayerHandler ph : snapshot) {
            try { ph.conn.close(); } catch (Exception ignored) {}
        }
        lobby.reset();
        redScore.set(0); blueScore.set(0); greenScore.set(0); yellowScore.set(0);
        collectedThisBatch.clear();
        respawnBatchCount.set(0);
        System.out.println("[Server] Game over. Lobby reset and ready.");
    }

    private void disconnect() {
        playerHandlers.remove(this);
        lobby.removePlayer(playerId);
        System.out.println("[Lobby] " + playerId + " disconnected.");

        if (playerId != null) {
            broadcastAll(gson.toJson(JSON_GameMessage.disconnect(playerId)));
            if (playerHandlers.isEmpty() && lobby.getState() == GameLobby.State.IN_GAME) {
                lobby.reset();
                redScore.set(0); blueScore.set(0); greenScore.set(0); yellowScore.set(0);
                collectedThisBatch.clear();
                respawnBatchCount.set(0);
                System.out.println("[Server] All players left. Lobby reset.");
            }
            evaluateAutoStart();
        }
    }
}
