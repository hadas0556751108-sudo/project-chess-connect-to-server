package server;

import model.User;
import org.java_websocket.WebSocket;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SessionManagerTest {

    private final WebSocket conn = mock(WebSocket.class);

    @Test
    void addAndGetSession() {
        SessionManager manager = new SessionManager();
        User user = new User("alice", "hash", 1200);
        manager.addSession(conn, user);

        Session byConn = manager.getSession(conn);
        Session byName = manager.getSession("alice");

        assertNotNull(byConn);
        assertSame(byConn, byName);
        assertEquals("alice", byConn.getUsername());
    }

    @Test
    void markDisconnectedAndReconnect() {
        SessionManager manager = new SessionManager();
        User user = new User("alice", "hash", 1200);
        WebSocket newConn = mock(WebSocket.class);

        manager.addSession(conn, user);
        manager.getSession(conn).setRoomId(5);

        Session disconnected = manager.markDisconnected(conn);
        assertNotNull(disconnected);
        assertTrue(manager.isDisconnected("alice"));
        assertNull(manager.getSession(conn));

        Session reconnected = manager.reconnect("alice", newConn, user);
        assertNotNull(reconnected);
        assertEquals(5, reconnected.getRoomId());
        assertSame(reconnected, manager.getSession("alice"));
    }

    @Test
    void removeSession() {
        SessionManager manager = new SessionManager();
        User user = new User("alice", "hash", 1200);
        manager.addSession(conn, user);

        manager.removeSession(conn);
        assertNull(manager.getSession(conn));
        assertNull(manager.getSession("alice"));
        assertFalse(manager.isDisconnected("alice"));
    }

    @Test
    void removeDisconnected() {
        SessionManager manager = new SessionManager();
        User user = new User("alice", "hash", 1200);
        manager.addSession(conn, user);
        manager.markDisconnected(conn);

        manager.removeDisconnected("alice");
        assertFalse(manager.isDisconnected("alice"));
    }

    @Test
    void getConnectionByUsername() {
        SessionManager manager = new SessionManager();
        User user = new User("alice", "hash", 1200);
        manager.addSession(conn, user);
        assertSame(conn, manager.getConnection("alice"));
    }

    @Test
    void connectionsInRoomOnlyOpen() {
        WebSocket open = mock(WebSocket.class);
        when(open.isOpen()).thenReturn(true);

        SessionManager manager = new SessionManager();
        User user = new User("alice", "hash", 1200);
        manager.addSession(open, user);
        manager.getSession(open).setRoomId(7);

        assertEquals(1, manager.getConnectionsInRoom(7).size());
        assertTrue(manager.getConnectionsInRoom(7).contains(open));
    }

    @Test
    void getAllConnections() {
        SessionManager manager = new SessionManager();
        manager.addSession(conn, new User("alice", "hash", 1200));
        assertEquals(1, manager.getAllConnections().size());
    }
}
