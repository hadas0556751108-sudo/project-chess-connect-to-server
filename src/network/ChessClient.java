package network;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class ChessClient extends WebSocketClient {
    private final BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();
    private final List<MessageHandler> handlers = new CopyOnWriteArrayList<>();

    public ChessClient(URI serverUri) {
        super(serverUri);
    }

    public void addMessageHandler(MessageHandler handler) {
        handlers.add(handler);
    }

    public void removeMessageHandler(MessageHandler handler) {
        handlers.remove(handler);
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        ClientLogger.getInstance().log("Connected to server");
    }

    @Override
    public void onMessage(String message) {
        messageQueue.offer(message);
        ClientLogger.getInstance().log("RECV: " + message + " | handlers: " + handlers.size());
        for (MessageHandler handler : handlers) {
            ClientLogger.getInstance().log("DISPATCH to " + handler.getClass().getName());
            handler.handleMessage(message);
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        ClientLogger.getInstance().log("Disconnected from server: " + reason);
    }

    @Override
    public void onError(Exception ex) {
        ClientLogger.getInstance().logErr("Client error: " + ex.getMessage());
    }

    public void sendCommand(String command) {
        ClientLogger.getInstance().log("SEND: " + command);
        if (command != null && !command.isEmpty()) {
            send(command);
        }
    }

    public String nextMessage(long timeoutMs) throws InterruptedException {
        return messageQueue.poll(timeoutMs, TimeUnit.MILLISECONDS);
    }

    public boolean hasMessage() {
        return !messageQueue.isEmpty();
    }
}
