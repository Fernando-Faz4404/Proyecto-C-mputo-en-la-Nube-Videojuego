package main;

import java.net.Socket;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.IOException;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.List;
import json.JSON_LoadPlayers;

import json.JSON_NewPlayer;
import json.JSON_Player;
import json.JSON_PlayerAuthentication;


public class PlayerHandler implements Runnable {
    public static CopyOnWriteArrayList <PlayerHandler> playerHandlers = new CopyOnWriteArrayList<>();
    
//    Variable que utilizare para asignarle un identificador unico a cada jugador
    private static AtomicInteger numPlayers = new AtomicInteger(1);    
    
    private Socket socket;
    private BufferedWriter bw;
    private BufferedReader br;
    
    private int playerID;
    private String playerName;
    private int posX;
    private int posY;
    private String direction;
    
//    private Player player = new Player();
//    private int playerID;
//    private String playerName;
    
    
    
    //Se crea la info que el servidor mandara sobre el mapa 
    //Se crea una vez para evitar que se cree cada vez que entra un nuevo jugador  
    private static final GamePanel gp = new GamePanel();
    
    private Gson gson = new Gson();
    
    public PlayerHandler(Socket socket){
        
        try{
            this.socket = socket;
            this.bw = new BufferedWriter(new OutputStreamWriter(this.socket.getOutputStream()));
            this.br = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
            
            this.playerID = PlayerHandler.numPlayers.getAndIncrement();
            
//          Aqui recibe el nombre  
            this.playerName = br.readLine(); 
            
            // Posición inicial del jugador en el mapa del MUNDO (coordenadas X).
            this.posX = gp.getTileSize() * 22;
            
            // Posición inicial del jugador en el mapa del MUNDO (coordenadas Y).
            this.posY = gp.getTileSize() * 30;
            
            PlayerHandler.playerHandlers.add(this);
            
            //Se manda la info del mapa al cliente            
            this.sendMap();
            
            //Servidor le debe mandar su id al jugador
            authPlayer();
            
            //Servidor le manda la lista de jugadores existentes
            sendExistingPlayers();
            
            //Se le notifica a los clientes que se ha unido otro jugador
            this.notifyNewPlayer();
            
        }catch(IOException e){
            this.closeConnection(socket, br, bw);
        }
    
    }
    
    @Override
    public void run(){
        String messageFromPlayer;
        while(this.socket.isConnected()){
            try{
//              Por mientras solo recibe el mensaje sobre la posicion del jugador
                messageFromPlayer = br.readLine();
                
//                System.out.println(messageFromPlayer);
                
                if(messageFromPlayer != null){
                    handlerClientMessages(messageFromPlayer);
                    this.broadcastMessage(messageFromPlayer);
                }
                
            }
            catch(IOException e){
                this.closeConnection(socket, br, bw);
                break;
            }
        }
    }
    
    public void broadcastMessage(String message){
        for(PlayerHandler playerHandler : PlayerHandler.playerHandlers){
            if(playerHandler.playerID != this.playerID){
                sendMessagePlayer(playerHandler, message);
            }    
        }
    }
    
    public void sendMessagePlayer(PlayerHandler playerHandler, String message){
        try{
            playerHandler.bw.write(message);
            playerHandler.bw.newLine();
            playerHandler.bw.flush();    
            }catch(IOException e){
                closeConnection(playerHandler.socket, playerHandler.br, playerHandler.bw);
            }
    }
    
    public void notifyNewPlayer(){
        JSON_NewPlayer message = new JSON_NewPlayer(this.playerID, this.playerName, this.posX, this.posY); 
        System.out.println(message);
        this.broadcastMessage(gson.toJson(message));
    }
    
    public void sendMap(){
        String jsonMap = gson.toJson(gp);
        
        System.out.println(jsonMap);
        
        try{
            this.bw.write(jsonMap);
            this.bw.newLine();
            this.bw.flush();
        }catch(IOException e){
            e.printStackTrace();
        }
    }
    
//  Le manda el json de autenticacion al cliente  
    public void authPlayer(){
        JSON_PlayerAuthentication jpa = new JSON_PlayerAuthentication();
        jpa.playerID = this.playerID;
        
        sendMessagePlayer(this, gson.toJson(jpa, JSON_PlayerAuthentication.class));
        
    }

    public void closeConnection(Socket socket, BufferedReader br, BufferedWriter bw){
        playerHandlers.remove(this);
        
        try{
            socket.close();
            br.close();
            bw.close();
        }catch(IOException e){
            e.printStackTrace();
        }
    }
    
    public void sendExistingPlayers(){
        List<Player> playersList = new java.util.ArrayList<>();
        
        for(PlayerHandler other : PlayerHandler.playerHandlers){
            if(other.playerID != this.playerID){
                playersList.add(new Player(other.playerID, other.playerName, other.posX, other.posY, other.direction));
            }
        }
        
        JSON_LoadPlayers jlp = new JSON_LoadPlayers(playersList);
        
        // Convertimos a JSON y enviamos un ÚNICO mensaje
        String jsonResponse = gson.toJson(jlp);
        
        sendMessagePlayer(this, jsonResponse);
        
    }
    
    public void handlerClientMessages(String json){
        
//      Obtenemos de manera generica el json
        JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
        
//      Obtenemos el campo type que dice que tipo de json es
        String type = jsonObject.get("type").getAsString();
        
        switch(type){
            case "PLAYER_MOVED":

                JSON_Player data = gson.fromJson(json, JSON_Player.class);
                
                //  Actualizo mis datos debido a que yo me he movido
                this.posX = data.posX;
                this.posY = data.posY;
                this.direction = data.direction;
                
                //Se mueve pero aun asi se le debe actualizar a los demas jugadores
                //Se actualiza el tipo de mensaje que es                
                
                break;
                
            default:
                break;    
            
        }
    }
}
