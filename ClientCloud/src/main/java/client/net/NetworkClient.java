package client.net;

import client.entity.Team;
import com.google.gson.Gson;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;

/**
 * Cliente de red basado en WebSocket.
 * Reemplaza la implementación anterior de TCP raw (Socket + DataInputStream/DataOutputStream).
 * Compatible con Cloudflare en puerto 443 (wss://).
 */
public class NetworkClient {

    public interface EventListener {
        void onLobbyState(GameMessage msg);
        void onGameStart(GameMessage msg);
        void onRemoteTankUpdate(String id, Team team, double x, double y,
                                double angle, int health, boolean alive);
        void onRemoteBullet(double x, double y, double angle, String ownerId, Team team);
        void onRemoteDeath(String id);
        void onRemoteDisconnect(String id);
        void onScoreUpdate(int red, int blue, int green, int yellow);
        void onPowerUpCollected(int index);
        void onPowerUpRespawn(int batchIndex);
        void onRoundEnd(int round, int total, String winner,
                        int redWins, int blueWins, int greenWins, int yellowWins);
        void onRoundStart(int round, int total, String mapResource, long seed,
                          int redWins, int blueWins, int greenWins, int yellowWins);
    }

    private final String serverUrl;
    private final Gson gson = new Gson();

    private EventListener listener;
    private WebSocketClient ws;
    private volatile boolean connected = false;

    // Guardados para el JOIN y para dispatch()
    private String pendingPlayerId;
    private String pendingTeam;
    private int pendingTeamCount;

    public NetworkClient(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public void setListener(EventListener l) { this.listener = l; }

    public void connect(String playerId, String team, int teamCount) {
        this.pendingPlayerId  = playerId;
        this.pendingTeam      = team;
        this.pendingTeamCount = teamCount;

        Thread t = new Thread(() -> {
            try {
                ws = new WebSocketClient(new URI(serverUrl)) {
                    @Override
                    public void onOpen(ServerHandshake handshakedata) {
                        connected = true;
                        System.out.println("[WS] Conectado a " + serverUrl);
                        // Enviar JOIN inmediatamente al abrir la conexión
                        send(gson.toJson(GameMessage.join(pendingPlayerId, pendingTeam, pendingTeamCount)));
                    }

                    @Override
                    public void onMessage(String message) {
                        try {
                            GameMessage msg = gson.fromJson(message, GameMessage.class);
                            dispatch(msg);
                        } catch (Exception e) {
                            System.err.println("[WS] Error al procesar mensaje: " + e.getMessage());
                        }
                    }

                    @Override
                    public void onClose(int code, String reason, boolean remote) {
                        connected = false;
                        System.out.println("[WS] Desconectado. code=" + code + " reason=" + reason);
                    }

                    @Override
                    public void onError(Exception ex) {
                        System.err.println("[WS] Error: " + ex.getMessage());
                    }
                };
                ws.connect();
            } catch (Exception e) {
                System.err.println("[WS] Falló la conexión: " + e.getMessage());
            }
        }, "WS-Connect-Thread");
        t.setDaemon(true);
        t.start();
    }

    // ---- dispatch — igual que antes ----

    private void dispatch(GameMessage msg) {
        if (msg.type == null || listener == null) return;
        switch (msg.type) {
            case LOBBY_STATE -> listener.onLobbyState(msg);
            case GAME_START  -> listener.onGameStart(msg);
            case MOVE, STATE_UPDATE, JOIN -> {
                if (msg.playerId != null && !msg.playerId.equals(pendingPlayerId)
                        && msg.team != null) {
                    listener.onRemoteTankUpdate(msg.playerId, Team.valueOf(msg.team),
                            msg.x != null ? msg.x : 0,
                            msg.y != null ? msg.y : 0,
                            msg.angle != null ? msg.angle : 0,
                            msg.health != null ? msg.health : 100,
                            msg.alive == null || msg.alive);
                }
            }
            case SHOOT -> {
                if (msg.playerId != null && !msg.playerId.equals(pendingPlayerId)
                        && msg.team != null) {
                    listener.onRemoteBullet(msg.x, msg.y, msg.angle,
                            msg.playerId, Team.valueOf(msg.team));
                }
            }
            case DEATH -> {
                if (msg.playerId != null && !msg.playerId.equals(pendingPlayerId))
                    listener.onRemoteDeath(msg.playerId);
            }
            case DISCONNECT -> {
                if (msg.playerId != null) listener.onRemoteDisconnect(msg.playerId);
            }
            case SCORE_UPDATE -> listener.onScoreUpdate(
                    msg.redScore    != null ? msg.redScore    : 0,
                    msg.blueScore   != null ? msg.blueScore   : 0,
                    msg.greenScore  != null ? msg.greenScore  : 0,
                    msg.yellowScore != null ? msg.yellowScore : 0);
            case POWERUP_COLLECTED -> {
                if (msg.powerUpIndex != null) listener.onPowerUpCollected(msg.powerUpIndex);
            }
            case POWERUP_RESPAWN -> {
                if (msg.powerUpRespawnBatch != null)
                    listener.onPowerUpRespawn(msg.powerUpRespawnBatch);
            }
            case ROUND_END -> listener.onRoundEnd(
                    msg.roundNumber != null ? msg.roundNumber : 1,
                    msg.totalRounds != null ? msg.totalRounds : 3,
                    msg.roundWinner != null ? msg.roundWinner : "DRAW",
                    msg.redWins    != null ? msg.redWins    : 0,
                    msg.blueWins   != null ? msg.blueWins   : 0,
                    msg.greenWins  != null ? msg.greenWins  : 0,
                    msg.yellowWins != null ? msg.yellowWins : 0);
            case ROUND_START -> listener.onRoundStart(
                    msg.roundNumber != null ? msg.roundNumber : 2,
                    msg.totalRounds != null ? msg.totalRounds : 3,
                    msg.mapResource != null ? msg.mapResource : "/maps/bigBattleMap.txt",
                    msg.seed        != null ? msg.seed        : System.currentTimeMillis(),
                    msg.redWins    != null ? msg.redWins    : 0,
                    msg.blueWins   != null ? msg.blueWins   : 0,
                    msg.greenWins  != null ? msg.greenWins  : 0,
                    msg.yellowWins != null ? msg.yellowWins : 0);
            default -> {}
        }
    }

    // ---- API pública ----

    public boolean isConnected() {
        return ws != null && ws.isOpen();
    }

    public void send(GameMessage msg) {
        if (isConnected()) {
            ws.send(gson.toJson(msg));
        }
    }

    public void disconnect() {
        if (ws != null) ws.close();
    }

    public String getPlayerId() { return pendingPlayerId; }
}
