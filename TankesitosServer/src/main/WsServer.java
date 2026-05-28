package main;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;

/**
 * Servidor WebSocket que reemplaza el ServerSocket/TCP raw original.
 * Escucha en el puerto indicado y delega toda la lógica de mensajes
 * a {@link PlayerHandler}.
 */
public class WsServer extends WebSocketServer {

    public WsServer(int port) {
        super(new InetSocketAddress("0.0.0.0", port));
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("[WS] Nueva conexión desde: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        PlayerHandler.onMessage(conn, message);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("[WS] Conexión cerrada: " + conn.getRemoteSocketAddress()
                + " code=" + code + " reason=" + reason);
        PlayerHandler.onClose(conn);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("[WS] Error en conexión "
                + (conn != null ? conn.getRemoteSocketAddress() : "null") + ": " + ex.getMessage());
    }

    @Override
    public void onStart() {
        System.out.println("[WS] WS server listo en :8080");
    }
}
