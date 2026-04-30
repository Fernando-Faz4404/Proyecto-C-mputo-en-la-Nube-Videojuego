package client.net;

import client.entity.Bullet;
import client.entity.Tank;
import client.entity.Team;
import client.game.GamePanel;
import com.google.gson.Gson;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

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
                socket = new Socket(InetAddress.getByName(serverIp), serverPort);
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
}

