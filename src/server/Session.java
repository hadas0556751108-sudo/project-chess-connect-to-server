package server;

import model.User;
import org.java_websocket.WebSocket;

public class Session {
    private final User user;
    private WebSocket connection;
    private volatile int roomId = -1;
    private String color;

    public Session(User user, WebSocket connection) {
        this.user = user;
        this.connection = connection;
    }

    public User getUser() {
        return user;
    }

    public String getUsername() {
        return user.getUsername();
    }

    public int getElo() {
        return user.getElo();
    }

    public WebSocket getConnection() {
        return connection;
    }

    public void setConnection(WebSocket connection) {
        this.connection = connection;
    }

    public int getRoomId() {
        return roomId;
    }

    public void setRoomId(int roomId) {
        this.roomId = roomId;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public boolean isInRoom() {
        return roomId >= 0;
    }

    public void leaveRoom() {
        this.roomId = -1;
    }
}
