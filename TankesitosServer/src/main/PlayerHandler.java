package main;

import java.net.Socket;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import json.JSON_GameMessage;

public class PlayerHandler implements Runnable {
    public static CopyOnWriteArrayList<PlayerHandler> playerHandlers = new CopyOnWriteArrayList<>();

    private static AtomicInteger numPlayers = new AtomicInteger(1);
    private static AtomicInteger redScore   = new AtomicInteger(0);
    private static AtomicInteger blueScore  = new AtomicInteger(0);

    private Socket socket;
    private DataOutputStream dos;
    private DataInputStream  dis;

    private int    playerID;
    private String playerId;   // ID string recibido del cliente en JOIN
    private String team;
    private double posX;
    private double posY;
    private double angle;
    private int    health = 100;
    private boolean alive = true;

    // Se crea una vez para calcular el tileSize de posición inicial
    private static final GamePanel gp = new GamePanel();

    private Gson gson = new Gson();

    public PlayerHandler(Socket socket) {
        try {
            this.socket = socket;
            this.dos    = new DataOutputStream(socket.getOutputStream());
            this.dis    = new DataInputStream(socket.getInputStream());
            this.playerID = PlayerHandler.numPlayers.getAndIncrement();

            // Recibe el mensaje JOIN del cliente (DataInputStream.readUTF)
            String joinJson = dis.readUTF();
            JsonObject joinObj = JsonParser.parseString(joinJson).getAsJsonObject();
            this.playerId = joinObj.has("playerId") ? joinObj.get("playerId").getAsString()
                                                    : "Player" + this.playerID;
            this.team     = joinObj.has("team")     ? joinObj.get("team").getAsString() : "BLUE";

            // Posición inicial según equipo (coordenadas del mapa del cliente: 25x16 tiles, tileSize=48)
            this.posX = "RED".equals(this.team) ? gp.getTileSize() * 3 : gp.getTileSize() * 21;
            this.posY = gp.getTileSize() * 8;

            PlayerHandler.playerHandlers.add(this);
            System.out.println("Se ha unido: " + this.playerId + " [" + this.team + "]");

            // Manda el estado de jugadores ya conectados al nuevo jugador
            sendExistingPlayers();

            // Notifica a los demás del nuevo jugador
            notifyNewPlayer();

        } catch (IOException e) {
            closeConnection(socket, dis, dos);
        }
    }

    @Override
    public void run() {
        while (socket.isConnected()) {
            try {
                String messageFromPlayer = dis.readUTF();
                if (messageFromPlayer != null) {
                    handlerClientMessages(messageFromPlayer);
                }
            } catch (IOException e) {
                closeConnection(socket, dis, dos);
                break;
            }
        }
    }

    public void broadcastMessage(String message) {
        for (PlayerHandler playerHandler : PlayerHandler.playerHandlers) {
            if (playerHandler.playerID != this.playerID) {
                sendMessagePlayer(playerHandler, message);
            }
        }
    }

    public void sendMessagePlayer(PlayerHandler playerHandler, String message) {
        try {
            playerHandler.dos.writeUTF(message);
            playerHandler.dos.flush();
        } catch (IOException e) {
            closeConnection(playerHandler.socket, playerHandler.dis, playerHandler.dos);
        }
    }

    public void notifyNewPlayer() {
        // Envía el estado de este jugador a todos los demás (STATE_UPDATE)
        JSON_GameMessage msg = JSON_GameMessage.stateUpdate(
                this.playerId, this.team, this.posX, this.posY, this.angle, this.health, this.alive);
        broadcastMessage(gson.toJson(msg));
    }

    public void sendExistingPlayers() {
        // Envía el estado de cada jugador existente al recién conectado
        for (PlayerHandler other : PlayerHandler.playerHandlers) {
            if (other.playerID != this.playerID) {
                JSON_GameMessage msg = JSON_GameMessage.stateUpdate(
                        other.playerId, other.team, other.posX, other.posY,
                        other.angle, other.health, other.alive);
                sendMessagePlayer(this, gson.toJson(msg));
            }
        }
    }

    public void closeConnection(Socket socket, DataInputStream dis, DataOutputStream dos) {
        playerHandlers.remove(this);

        // Notifica a los demás la desconexión
        if (this.playerId != null) {
            System.out.println("Se ha desconectado: " + this.playerId);
            String disconnectJson = gson.toJson(JSON_GameMessage.disconnect(this.playerId));
            for (PlayerHandler playerHandler : PlayerHandler.playerHandlers) {
                sendMessagePlayer(playerHandler, disconnectJson);
            }
        }

        try {
            if (socket != null) socket.close();
            if (dis    != null) dis.close();
            if (dos    != null) dos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void handlerClientMessages(String json) {
        JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
        String type = jsonObject.get("type").getAsString();

        switch (type) {
            case "MOVE": {
                // Actualiza el estado almacenado y reenvía a los demás
                JSON_GameMessage data = gson.fromJson(json, JSON_GameMessage.class);
                if (data.x      != null) this.posX   = data.x;
                if (data.y      != null) this.posY   = data.y;
                if (data.angle  != null) this.angle  = data.angle;
                if (data.health != null) this.health = data.health;
                if (data.alive  != null) this.alive  = data.alive;
                broadcastMessage(json);
                break;
            }
            case "SHOOT": {
                broadcastMessage(json);
                break;
            }
            case "DEATH": {
                this.alive  = false;
                this.health = 0;
                // El equipo contrario suma un punto
                int red  = redScore.get();
                int blue = blueScore.get();
                if ("RED".equals(this.team)) {
                    blue = blueScore.incrementAndGet();
                } else {
                    red  = redScore.incrementAndGet();
                }
                String scoreJson = gson.toJson(JSON_GameMessage.scoreUpdate(red, blue));
                for (PlayerHandler ph : PlayerHandler.playerHandlers) {
                    sendMessagePlayer(ph, scoreJson);
                }
                broadcastMessage(json);
                break;
            }
            default:
                break;
        }
    }
}
