package main;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import entidad.JugadorSecundario;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Scanner;

import json.*;

public class Cliente {
    private Socket socket;
    private Gson gson = new Gson();
    private BufferedReader br;
    private BufferedWriter bw;
    
    private GamePanel gp;
    
//  Elementos para reconocer especificamente a un cliente
    private String playerName;
    private int playerID;
    
    public Cliente(Socket socket, GamePanel gp){
        try{
            //this.usuario = usuario;
            this.gp = gp;
            this.socket = socket;
            this.br = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
            this.bw = new BufferedWriter(new OutputStreamWriter(this.socket.getOutputStream())); 
        
        }catch(IOException e){
            this.closeConnection(socket, br, bw);
        }
    }
    
    public void manejadorMensajesServidor(String json){
        
//      Obtenemos de manera generica el json
        JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
        
//      Obtenemos el campo type que dice que tipo de json es
        String type = jsonObject.get("type").getAsString();
        
//        System.out.println(type);
        
        switch(type){
            case "MAP_INIT":
                JSON_GamePanel data = gson.fromJson(json, JSON_GamePanel.class);
       
                gp.setearValoresGamePanel(data);
                break;
                
            case "PLAYER_MOVE":
                JSON_Player dataJP = gson.fromJson(json, JSON_Player.class);
                JugadorSecundario aux = gp.otrosJugadores.get(dataJP.id);
    
                System.out.println(json);
                
                if (aux != null) {
                    aux.setPosX(dataJP.posX);
                    aux.setPosY(dataJP.posY);
                    aux.setDireccion(dataJP.direccion);
                }
                break;
            case "NEW_PLAYER":
//              
                System.out.println("Se unio otro jugador!");
                System.out.println("Esto recibo del servidor: "+json);
                JSON_NewPlayer dataJNP = gson.fromJson(json, JSON_NewPlayer.class);
                
                JugadorSecundario jugadorSecundario = new JugadorSecundario();
                
                jugadorSecundario.playerID = dataJNP.playerID;
                jugadorSecundario.playerName = dataJNP.playerName;

//              Debo cambiar estoooooo  
                
                jugadorSecundario.setPosX( dataJNP.posX);
                jugadorSecundario.setPosY(dataJNP.posY);
                
                System.out.println(dataJNP.posX);
                System.out.println(dataJNP.posY);
                
                System.out.println(jugadorSecundario.posX);
                System.out.println(jugadorSecundario.posY);
                
                gp.otrosJugadores.put(dataJNP.playerID, jugadorSecundario);
                System.out.println(gson.toJson(jugadorSecundario));
                break;
                
            case "AUTH":
//              El servidor mando el id de autenticacion del jugador  
                JSON_PlayerAuthentication dataJPA = gson.fromJson(json, JSON_PlayerAuthentication.class);
                this.playerID = dataJPA.playerID; 
                System.out.println("mi id"+this.playerID);
                break;  
                
             case "LOAD_PLAYERS":
//              El servidor mando una lista de jugadores
                JSON_LoadPlayers dataJLP = gson.fromJson(json, JSON_LoadPlayers.class);
                for(JugadorSecundario otro : dataJLP.players){
                    gp.otrosJugadores.put(otro.playerID, otro);
                }
                break;
 
        }
    }
    
    public void ingresarVideojuego(){
         try{
            Scanner teclado = new Scanner(System.in);
            String mensajeServidor;
            
            System.out.print("Ingresa el nombre de jugador: ");
            playerName = teclado.nextLine();
            
            //Mandamos el playerName al servidor
            this.bw.write(playerName);
            this.bw.newLine();
            this.bw.flush();
            
//          Empieza a recibir mensajes del servidor
//            recibeMensajeServidor();
//          Despues de mandar nuestro usuario el servidor manda la conf del mapa
            mensajeServidor = br.readLine();
            System.out.println(mensajeServidor);
            
            if(mensajeServidor != null){
                manejadorMensajesServidor(mensajeServidor);
            }else{
                System.out.println("El servidor no mando la conf del mundo");
            }

//          Una vez seteado el mapa
            recibeMensajeServidor();
          
        }catch(IOException e){
            e.printStackTrace();
        } 
    
    }
    
    public void enviaMensaje(String mensaje){
        try{
            //this.enviaMsg.write(this.usuario + " :  "+ mensajeParaEnviar);
            this.bw.write(mensaje);
            this.bw.newLine();
            this.bw.flush();
        }catch(IOException e){
            closeConnection(this.socket, this.br, this.bw);
        }
    }
    
    public void recibeMensajeServidor(){
        new Thread(new Runnable(){
            @Override
            public void run(){
                String mensajeServidor;
                while(socket.isConnected()){
                    try{
                        //Recibimos el JSON
                        mensajeServidor = br.readLine();
                        System.out.println(mensajeServidor);
                        if(mensajeServidor != null){
                            manejadorMensajesServidor(mensajeServidor);
                        }
                        
                    }catch(IOException e){
                        closeConnection(socket, br, bw);
                        break;
                    }
                }  
            }
        }).start();
    }
    
    public void closeConnection(Socket socket, BufferedReader br, BufferedWriter bw){
        try{
            socket.close();
            br.close();
            bw.close();
        }catch(IOException e){
            e.printStackTrace();
        }
    }
    
    
    public int getIdCliente(){
        return this.playerID;
    }
    
     public String getNombreCliente(){
        return this.playerName;
    }
    
}
