package main;

/**
 * Punto de entrada del servidor.
 * Arranca un servidor WebSocket en el puerto 8080.
 * Ejecutar con: java -jar target/TankesitosServer-1.0.jar
 */
public class TankesitosServer {

    public static void main(String[] args) {
        WsServer server = new WsServer(8080);
        server.setReuseAddr(true);
        server.start();
        // El servidor corre indefinidamente; la JVM no termina.
    }
}
