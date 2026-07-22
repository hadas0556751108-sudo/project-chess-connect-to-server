package server;

import model.User;
import org.java_websocket.WebSocket;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class SessionManager {
    private final Map<WebSocket, Session> sessions = new ConcurrentHashMap<>();
    private final Map<String, Session> byUsername = new ConcurrentHashMap<>();
    private final Map<String, Session> disconnected = new ConcurrentHashMap<>();

    public void addSession(WebSocket connection, User user) {
        Session session = new Session(user, connection);
        sessions.put(connection, session);
        byUsername.put(user.getUsername(), session);
    }

    public Session reconnect(String username, WebSocket newConnection, model.User user) {
        Session old = disconnected.remove(username);
        if (old == null) {
            return null;
        }
        Session session = new Session(user, newConnection);
        session.setRoomId(old.getRoomId());
        session.setColor(old.getColor());
        sessions.put(newConnection, session);
        byUsername.put(username, session);
        return session;
    }

    public Session markDisconnected(WebSocket connection) {
        Session session = sessions.remove(connection);
        if (session != null) {
            byUsername.remove(session.getUsername());
            disconnected.put(session.getUsername(), session);
            return session;
        }
        return null;
    }

    public void removeSession(WebSocket connection) {
        Session session = sessions.remove(connection);
        if (session != null) {
            byUsername.remove(session.getUsername());
            disconnected.remove(session.getUsername());
        }
    }

    public Session getSession(WebSocket connection) {
        return sessions.get(connection);
    }

    public Session getSession(String username) {
        return byUsername.get(username);
    }

    public Session getDisconnected(String username) {
        return disconnected.get(username);
    }

    public boolean isDisconnected(String username) {
        return disconnected.containsKey(username);
    }

    public void removeDisconnected(String username) {
        disconnected.remove(username);
    }

    public WebSocket getConnection(String username) {
        Session session = byUsername.get(username);
        return session == null ? null : session.getConnection();
    }

    public Collection<WebSocket> getConnectionsInRoom(int roomId) {
        return sessions.values().stream()
                .filter(s -> s.getRoomId() == roomId)
                .map(Session::getConnection)
                .filter(c -> c != null && c.isOpen())
                .collect(Collectors.toList());
    }

    public Collection<WebSocket> getAllConnections() {
        return sessions.keySet();
    }
}
