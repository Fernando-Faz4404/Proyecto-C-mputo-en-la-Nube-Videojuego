package main;

import java.io.IOException;
import java.net.ServerSocket;

public class TankesitosServer {

  
    public static void main(String[] args) {
        try{
            //Creamos el socket server            
            ServerSocket serverSocket = new ServerSocket(2555);
            Server server = new Server(serverSocket);
            
            server.startServer();
            
        }catch(IOException e){
            e.printStackTrace();
        }
    }
    
}
