package client.net;

import client.entity.Bullet;
import client.entity.Tank;
import client.entity.Team;
import client.game.GamePanel;
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

/**
 * Multiplayer socket client. Uses DataOutputStream.writeUTF() /
 * DataInputStream.readUTF() with Gson JSON — same pattern as
 * the original Manda/Recibe classes.
 */
public class NetworkClient {

    private final String serverIp;
    private final int serverPort;
    private final GamePanel gamePanel;
    private final Gson gson = new Gson();

    private Socket socket;
    private DataOutputStream dos;
    private DataInputStream dis;
    private volatile boolean connected;

    public NetworkClient(String serverIp, int serverPort, GamePanel gamePanel) {
        this.serverIp = serverIp;
        this.serverPort = serverPort;
        this.gamePanel = gamePanel;
    }

    public void connect() {
        Thread t = new Thread(() -> {
            try {
                socket = new Socket(Proxy.NO_PROXY);
                InetAddress localBind = findLocalAddressForServer(serverIp);
                if (localBind != null) {
                    System.out.println("Binding local interface: " + localBind.getHostAddress());
                    socket.bind(new InetSocketAddress(localBind, 0));
                }
                socket.connect(new InetSocketAddress(serverIp, serverPort), 5000);
                dos = new DataOutputStream(socket.getOutputStream());
                dis = new DataInputStream(socket.getInputStream());
                connected = true;

                // Send JOIN message
                Tank local = gamePanel.getLocalTank();
                send(GameMessage.join(local.getPlayerId(), local.getTeam().name()));

                // Start receive thread
                Thread recibe = new Thread(this::receiveLoop, "Recibe-Game");
                recibe.setDaemon(true);
                recibe.start();

                System.out.println("Connected to " + serverIp + ":" + serverPort);
            } catch (IOException e) {
                System.out.println("Could not connect: " + e.getMessage() + " — running offline");
            }
        }, "Manda-Connect");
        t.setDaemon(true);
        t.start();
    }

    private void receiveLoop() {
        while (connected) {
            try {
                GameMessage msg = gson.fromJson(dis.readUTF(), GameMessage.class);
                handleMessage(msg);
            } catch (IOException e) {
                System.out.println("Disconnected from server.");
                connected = false;
            }
        }
    }

    private void handleMessage(GameMessage msg) {
        String localId = gamePanel.getLocalTank().getPlayerId();
        switch (msg.type) {
            case MOVE, STATE_UPDATE, JOIN -> {
                if (msg.playerId != null && !msg.playerId.equals(localId)) {
                    Team team = Team.valueOf(msg.team);
                    gamePanel.onRemoteTankUpdate(msg.playerId, team,
                            msg.x != null ? msg.x : 0,
                            msg.y != null ? msg.y : 0,
                            msg.angle != null ? msg.angle : 0,
                            msg.health != null ? msg.health : Tank.MAX_HEALTH,
                            msg.alive == null || msg.alive);
                }
            }
            case SHOOT -> {
                if (msg.playerId != null && !msg.playerId.equals(localId)) {
                    gamePanel.onRemoteBullet(msg.x, msg.y, msg.angle,
                            msg.playerId, Team.valueOf(msg.team));
                }
            }
            case DISCONNECT -> {
                if (msg.playerId != null) gamePanel.onRemoteDisconnect(msg.playerId);
            }
            case SCORE_UPDATE -> {
                gamePanel.onScoreUpdate(
                        msg.redScore != null ? msg.redScore : 0,
                        msg.blueScore != null ? msg.blueScore : 0);
            }
            default -> {}
        }
    }

    private synchronized void send(GameMessage msg) {
        if (!connected) return;
        try {
            dos.writeUTF(gson.toJson(msg));
            dos.flush();
        } catch (IOException e) {
            System.out.println("Send error: " + e.getMessage());
            connected = false;
        }
    }

    public void sendMove(Tank tank) {
        send(GameMessage.move(tank.getPlayerId(), tank.getTeam().name(),
                tank.getX(), tank.getY(), tank.getAngle(),
                tank.getHealth(), tank.isAlive()));
    }

    public void sendShoot(Bullet b) {
        send(GameMessage.shoot(b.getOwnerId(), b.getOwnerTeam().name(),
                b.getX(), b.getY(), b.getAngle()));
    }

    public void sendDeath(Tank tank) {
        send(GameMessage.death(tank.getPlayerId()));
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
                    byte[] mask  = prefixToMask(ia.getNetworkPrefixLength());
                    boolean sameSubnet = true;
                    for (int i = 0; i < 4; i++) {
                        if ((local[i] & mask[i]) != (remote[i] & mask[i])) {
                            sameSubnet = false; break;
                        }
                    }
                    if (sameSubnet) return ia.getAddress();
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

