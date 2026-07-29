package server;

import auth.AuthManager;
import model.User;
import network.MatchmakingClient;
import network.Protocol;
import org.java_websocket.WebSocket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import redis.RedisClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommandRouterTest {

    @Mock
    private AuthManager authManager;

    @Mock
    private RedisClient redisClient;

    @Mock
    private MatchmakingClient matchmakingClient;

    private CommandRouter router;

    @BeforeEach
    void setUp() {
        router = new CommandRouter(authManager, redisClient, matchmakingClient, "test-server");
    }

    private WebSocket openConn() {
        WebSocket conn = mock(WebSocket.class);
        lenient().when(conn.isOpen()).thenReturn(true);
        return conn;
    }

    private String login(WebSocket conn, String username) {
        when(authManager.authenticate(username, "pass")).thenReturn(new User(username, "hash", 1200));
        when(authManager.generateToken(any())).thenReturn("token");
        String response = router.route(conn, "LOGIN " + username + " pass");
        assertTrue(response.startsWith(Protocol.LOGIN_SUCCESS), response);
        return response;
    }

    @Test
    void emptyMessage() {
        assertEquals(Protocol.ERROR + " empty message", router.route(openConn(), ""));
    }

    @Test
    void ping() {
        assertEquals(Protocol.PONG, router.route(openConn(), "PING"));
    }

    @Test
    void unknownCommand() {
        assertEquals(Protocol.UNKNOWN_COMMAND + " FOO", router.route(openConn(), "FOO"));
    }

    @Test
    void registerSuccess() {
        when(authManager.register("alice", "secret")).thenReturn(true);
        assertEquals(Protocol.REGISTER_SUCCESS + " alice", router.route(openConn(), "REGISTER alice secret"));
    }

    @Test
    void registerFails() {
        when(authManager.register("alice", "secret")).thenReturn(false);
        assertTrue(router.route(openConn(), "REGISTER alice secret").startsWith(Protocol.REGISTER_FAILED));
    }

    @Test
    void loginSuccess() {
        WebSocket conn = openConn();
        assertEquals(Protocol.LOGIN_SUCCESS + " alice 1200 token", login(conn, "alice"));
    }

    @Test
    void loginWrongPassword() {
        when(authManager.authenticate("alice", "wrong")).thenReturn(null);
        assertTrue(router.route(openConn(), "LOGIN alice wrong").startsWith(Protocol.LOGIN_FAILED));
    }

    @Test
    void playCallsMatchmakingClient() {
        WebSocket conn = openConn();
        login(conn, "alice");
        when(matchmakingClient.enqueue("alice", 1200, "test-server")).thenReturn(true);

        assertNull(router.route(conn, "PLAY"));
        verify(matchmakingClient).enqueue("alice", 1200, "test-server");
    }

    @Test
    void playNotAuthenticated() {
        assertEquals(Protocol.NOT_AUTHENTICATED, router.route(openConn(), "PLAY"));
    }

    @Test
    void playAlreadyInRoom() {
        WebSocket conn = openConn();
        login(conn, "alice");
        router.route(conn, "CREATE_ROOM MyRoom");
        String response = router.route(conn, "PLAY");
        assertTrue(response.startsWith(Protocol.ALREADY_IN_ROOM));
    }

    @Test
    void playMatchmakingUnavailable() {
        WebSocket conn = openConn();
        login(conn, "alice");
        when(matchmakingClient.enqueue("alice", 1200, "test-server")).thenReturn(false);

        String response = router.route(conn, "PLAY");
        assertTrue(response.startsWith(Protocol.ERROR));
    }

    @Test
    void cancelPlayCallsMatchmakingClient() {
        WebSocket conn = openConn();
        login(conn, "alice");
        assertNull(router.route(conn, "CANCEL_PLAY"));
        verify(matchmakingClient).cancel("alice");
    }

    @Test
    void createRoom() {
        WebSocket conn = openConn();
        login(conn, "alice");
        String response = router.route(conn, "CREATE_ROOM MyRoom");
        assertTrue(response.startsWith(Protocol.ROOM_CREATED));
    }

    @Test
    void createRoomNotAuthenticated() {
        assertEquals(Protocol.NOT_AUTHENTICATED, router.route(openConn(), "CREATE_ROOM MyRoom"));
    }

    @Test
    void joinRoom() {
        WebSocket alice = openConn();
        WebSocket bob = openConn();
        login(alice, "alice");
        login(bob, "bob");

        String created = router.route(alice, "CREATE_ROOM MyRoom");
        int roomId = Integer.parseInt(created.split(" ")[1]);

        String response = router.route(bob, "JOIN_ROOM " + roomId);
        assertTrue(response.startsWith(Protocol.JOINED_ROOM));
        verify(bob).send(contains(Protocol.JOINED_ROOM));
    }

    @Test
    void joinRoomNotFound() {
        WebSocket conn = openConn();
        login(conn, "alice");
        assertEquals(Protocol.ROOM_NOT_FOUND, router.route(conn, "JOIN_ROOM 999"));
    }

    @Test
    void leaveRoom() {
        WebSocket conn = openConn();
        login(conn, "alice");
        String created = router.route(conn, "CREATE_ROOM MyRoom");
        int roomId = Integer.parseInt(created.split(" ")[1]);

        String response = router.route(conn, "LEAVE_ROOM");
        assertTrue(response.startsWith(Protocol.LEFT_ROOM + " " + roomId));
    }

    @Test
    void leaveRoomNotInRoom() {
        WebSocket conn = openConn();
        login(conn, "alice");
        assertEquals(Protocol.NOT_IN_ROOM, router.route(conn, "LEAVE_ROOM"));
    }

    @Test
    void listRoomsEmpty() {
        assertEquals(Protocol.ROOM_LIST, router.route(openConn(), "LIST_ROOMS"));
    }

    @Test
    void listRoomsWithRoom() {
        WebSocket conn = openConn();
        login(conn, "alice");
        router.route(conn, "CREATE_ROOM MyRoom");
        String list = router.route(openConn(), "LIST_ROOMS");
        assertTrue(list.startsWith(Protocol.ROOM_LIST));
        assertTrue(list.contains("MyRoom"));
    }

    @Test
    void startMatch() {
        WebSocket white = openConn();
        WebSocket black = openConn();
        login(white, "alice");
        login(black, "bob");

        router.startMatch("alice", "bob");

        verify(white).send(contains(Protocol.JOINED_ROOM));
        verify(black).send(contains(Protocol.JOINED_ROOM));
        verify(white).send(contains(Protocol.GAME_START));
        verify(black).send(contains(Protocol.GAME_START));
    }

    @Test
    void startMatchMissingSession() {
        router.startMatch("ghost1", "ghost2");
        // no exceptions and no messages
    }

    @Test
    void moveBroadcasted() {
        WebSocket white = openConn();
        WebSocket black = openConn();
        login(white, "alice");
        login(black, "bob");

        router.startMatch("alice", "bob");

        assertNull(router.route(white, "MOVE 6 0 5 0"));
        verify(white).send(contains("MOVE"));
        verify(black).send(contains("MOVE"));
    }

    @Test
    void moveNotInRoom() {
        WebSocket conn = openConn();
        login(conn, "alice");
        assertEquals(Protocol.NOT_IN_ROOM, router.route(conn, "MOVE 2 3 4 5"));
    }

    @Test
    void resignEndsGame() {
        WebSocket white = openConn();
        WebSocket black = openConn();
        login(white, "alice");
        login(black, "bob");

        when(authManager.getUser("alice")).thenReturn(new User("alice", "", 1200));
        when(authManager.getUser("bob")).thenReturn(new User("bob", "", 1200));

        router.startMatch("alice", "bob");
        assertNull(router.route(white, "RESIGN"));

        verify(white).send(contains(Protocol.GAME_OVER + " BLACK"));
        verify(black).send(contains(Protocol.GAME_OVER + " BLACK"));
    }

    @Test
    void gameOverEndsGame() {
        WebSocket white = openConn();
        WebSocket black = openConn();
        login(white, "alice");
        login(black, "bob");

        when(authManager.getUser("alice")).thenReturn(new User("alice", "", 1200));
        when(authManager.getUser("bob")).thenReturn(new User("bob", "", 1200));

        router.startMatch("alice", "bob");
        assertNull(router.route(white, "GAME_OVER WHITE"));

        verify(white).send(contains(Protocol.GAME_OVER + " WHITE"));
        verify(black).send(contains(Protocol.GAME_OVER + " WHITE"));
    }

    @Test
    void gameOverCannotDeclareOpponent() {
        WebSocket white = openConn();
        WebSocket black = openConn();
        login(white, "alice");
        login(black, "bob");

        router.startMatch("alice", "bob");
        String response = router.route(white, "GAME_OVER BLACK");
        assertTrue(response.startsWith(Protocol.ERROR));
    }

    @Test
    void disconnectRemovesSession() {
        WebSocket conn = openConn();
        login(conn, "alice");
        router.disconnect(conn);
        assertEquals(Protocol.NOT_AUTHENTICATED, router.route(conn, "PLAY"));
    }

    @Test
    void disconnectInRoomBroadcasts() {
        WebSocket white = openConn();
        WebSocket black = openConn();
        login(white, "alice");
        login(black, "bob");

        router.startMatch("alice", "bob");
        router.disconnect(white);

        verify(black).send(contains(Protocol.OPPONENT_DISCONNECTED));
    }

    @Test
    void reconnectionRestoresRoom() {
        WebSocket white = openConn();
        WebSocket black = openConn();
        WebSocket reconnect = openConn();
        login(white, "alice");
        login(black, "bob");

        router.startMatch("alice", "bob");
        router.disconnect(white);

        String response = login(reconnect, "alice");
        assertTrue(response.startsWith(Protocol.LOGIN_SUCCESS));
        verify(reconnect).send(contains(Protocol.JOINED_ROOM));
    }

    private static String contains(String expected) {
        return ArgumentMatchers.contains(expected);
    }
}
