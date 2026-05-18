package main;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    
    ServerSocket serverSocket;
    
    public Server(ServerSocket serverSocket){
        this.serverSocket = serverSocket;
    }
    
    
    // Esta funcion estara esperando hasta que un jugador nuevo se conecte 
    public void startServer(){
        while(!serverSocket.isClosed()){
 
            try{
// Aqui se detiene a esperar una conexion
// Si un jugador se conecta a nuestro server socket este regresa un socket 
                Socket socket = serverSocket.accept();
                System.out.println("Se ha unido un nuevo jugador"); 
                
                PlayerHandler playerHandler = new PlayerHandler(socket);
                Thread thread = new Thread(playerHandler);
                
                thread.start();
                 
                 
            }catch(IOException e){
                e.printStackTrace();
            }
        }
    
    }
    
    public void shutDownServer(){
        try{
            if(this.serverSocket != null){
                this.serverSocket.close();
            }
        }catch(IOException e){
    
        }
    }
}
