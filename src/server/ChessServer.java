package server;

import network.Protocol;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;

public class ChessServer extends WebSocketServer {
    private final CommandRouter router;

    public ChessServer(int port, CommandRouter router) {
        super(new InetSocketAddress(port));
        this.router = router;
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("New connection: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        router.disconnect(conn);
        System.out.println("Closed connection: " + conn.getRemoteSocketAddress() + " reason=" + reason);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        System.out.println("Received: " + message);
        String response = router.route(conn, message);
        if (response != null) {
            conn.send(response);
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        if (conn != null) {
            System.err.println("Error for " + conn.getRemoteSocketAddress() + ": " + ex.getMessage());
        } else {
            System.err.println("Server error: " + ex.getMessage());
        }
    }

    @Override
    public void onStart() {
        System.out.println("ChessServer started on port " + getPort());
    }
}
