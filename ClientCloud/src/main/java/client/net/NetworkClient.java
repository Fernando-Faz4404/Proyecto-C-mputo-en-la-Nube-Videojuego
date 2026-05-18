package client.net;

import client.entity.Team;
import com.google.gson.Gson;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.Proxy;
import java.net.Socket;
import java.util.Enumeration;

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

    private final String serverIp;
    private final int serverPort;
    private final Gson gson = new Gson();

    private EventListener listener;
    private Socket socket;
    private DataOutputStream dos;
    private DataInputStream dis;
    private volatile boolean connected;

    // Stored until connect() is called
    private String pendingPlayerId;
    private String pendingTeam;
    private int pendingTeamCount;

    public NetworkClient(String serverIp, int serverPort) {
        this.serverIp = serverIp;
        this.serverPort = serverPort;
    }

    public void setListener(EventListener l) { this.listener = l; }

    public void connect(String playerId, String team, int teamCount) {
        this.pendingPlayerId = playerId;
        this.pendingTeam = team;
        this.pendingTeamCount = teamCount;

        Thread t = new Thread(() -> {
            try {
                socket = new Socket(Proxy.NO_PROXY);
                InetAddress localBind = findLocalAddressForServer(serverIp);
                if (localBind != null) {
                    socket.bind(new InetSocketAddress(localBind, 0));
                }
                socket.connect(new InetSocketAddress(serverIp, serverPort), 5000);
                dos = new DataOutputStream(socket.getOutputStream());
                dis = new DataInputStream(socket.getInputStream());
                connected = true;

                send(GameMessage.join(pendingPlayerId, pendingTeam, pendingTeamCount));

                Thread recv = new Thread(this::receiveLoop, "Recv-Thread");
                recv.setDaemon(true);
                recv.start();

                System.out.println("Connected to " + serverIp + ":" + serverPort);
            } catch (IOException e) {
                System.out.println("Connection failed: " + e.getMessage());
            }
        }, "Connect-Thread");
        t.setDaemon(true);
        t.start();
    }

    private void receiveLoop() {
        while (connected) {
            try {
                GameMessage msg = gson.fromJson(dis.readUTF(), GameMessage.class);
                dispatch(msg);
            } catch (IOException e) {
                System.out.println("Disconnected from server.");
                connected = false;
            } catch (Exception e) {
                System.err.println("Message error: " + e.getMessage());
            }
        }
    }

    private void dispatch(GameMessage msg) {
        if (msg.type == null || listener == null) return;
        switch (msg.type) {
            case LOBBY_STATE -> listener.onLobbyState(msg);
            case GAME_START -> listener.onGameStart(msg);
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
                if (msg.playerId != null && !msg.playerId.equals(pendingPlayerId)) {
                    listener.onRemoteDeath(msg.playerId);
                }
            }
            case DISCONNECT -> {
                if (msg.playerId != null) listener.onRemoteDisconnect(msg.playerId);
            }
            case SCORE_UPDATE -> listener.onScoreUpdate(
                    msg.redScore != null ? msg.redScore : 0,
                    msg.blueScore != null ? msg.blueScore : 0,
                    msg.greenScore != null ? msg.greenScore : 0,
                    msg.yellowScore != null ? msg.yellowScore : 0);
            case POWERUP_COLLECTED -> {
                if (msg.powerUpIndex != null) listener.onPowerUpCollected(msg.powerUpIndex);
            }
            case POWERUP_RESPAWN -> {
                if (msg.powerUpRespawnBatch != null) listener.onPowerUpRespawn(msg.powerUpRespawnBatch);
            }
            case ROUND_END -> listener.onRoundEnd(
                    msg.roundNumber != null ? msg.roundNumber : 1,
                    msg.totalRounds != null ? msg.totalRounds : 3,
                    msg.roundWinner != null ? msg.roundWinner : "DRAW",
                    msg.redWins != null ? msg.redWins : 0,
                    msg.blueWins != null ? msg.blueWins : 0,
                    msg.greenWins != null ? msg.greenWins : 0,
                    msg.yellowWins != null ? msg.yellowWins : 0);
            case ROUND_START -> listener.onRoundStart(
                    msg.roundNumber != null ? msg.roundNumber : 2,
                    msg.totalRounds != null ? msg.totalRounds : 3,
                    msg.mapResource != null ? msg.mapResource : "/maps/bigBattleMap.txt",
                    msg.seed != null ? msg.seed : System.currentTimeMillis(),
                    msg.redWins != null ? msg.redWins : 0,
                    msg.blueWins != null ? msg.blueWins : 0,
                    msg.greenWins != null ? msg.greenWins : 0,
                    msg.yellowWins != null ? msg.yellowWins : 0);
            default -> {}
        }
    }

    public synchronized void send(GameMessage msg) {
        if (!connected) return;
        try {
            dos.writeUTF(gson.toJson(msg));
            dos.flush();
        } catch (IOException e) {
            System.err.println("Send error: " + e.getMessage());
            connected = false;
        }
    }

    public boolean isConnected() { return connected; }
    public String getPlayerId() { return pendingPlayerId; }

    public synchronized void disconnect() {
        connected = false;
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }

    private InetAddress findLocalAddressForServer(String remoteIp) {
        try {
            byte[] remote = InetAddress.getByName(remoteIp).getAddress();
            Enumeration<NetworkInterface> nifs = NetworkInterface.getNetworkInterfaces();
            while (nifs.hasMoreElements()) {
                NetworkInterface nif = nifs.nextElement();
                if (!nif.isUp() || nif.isLoopback()) continue;
                for (InterfaceAddress ia : nif.getInterfaceAddresses()) {
                    if (!(ia.getAddress() instanceof Inet4Address)) continue;
                    byte[] local = ia.getAddress().getAddress();
                    byte[] mask = prefixToMask(ia.getNetworkPrefixLength());
                    boolean same = true;
                    for (int i = 0; i < 4; i++) {
                        if ((local[i] & mask[i]) != (remote[i] & mask[i])) { same = false; break; }
                    }
                    if (same) return ia.getAddress();
                }
            }
        } catch (Exception e) {
            System.out.println("findLocalAddress error: " + e.getMessage());
        }
        return null;
    }

    private byte[] prefixToMask(int prefix) {
        byte[] mask = new byte[4];
        for (int i = 0; i < prefix; i++) mask[i / 8] |= (byte) (1 << (7 - i % 8));
        return mask;
    }
}
