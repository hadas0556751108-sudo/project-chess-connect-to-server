package server;

import auth.AuthManager;
import model.Position;
import model.User;
import network.MatchmakingClient;
import network.Protocol;
import org.java_websocket.WebSocket;
import redis.RedisClient;
import redis.clients.jedis.JedisPubSub;

import java.util.Arrays;
import java.util.Collection;

public class CommandRouter {
    private final AuthManager authManager;
    private final SessionManager sessionManager;
    private final RoomManager roomManager;
    private final EloManager eloManager;
    private final EventBus eventBus;
    private final MatchmakingManager matchmakingManager;
    private final DisconnectionManager disconnectionManager;
    private final RedisClient redisClient;
    private final MatchmakingClient matchmakingClient;
    private final String gameServerId;

    public CommandRouter(AuthManager authManager) {
        this(authManager, null, null, null);
    }

    public CommandRouter(AuthManager authManager, RedisClient redisClient, MatchmakingClient matchmakingClient, String gameServerId) {
        this.authManager = authManager;
        this.sessionManager = new SessionManager();
        this.roomManager = new RoomManager();
        this.eloManager = new EloManager(authManager);
        this.eventBus = new EventBus();
        initEventBus();
        this.redisClient = redisClient;
        this.matchmakingClient = matchmakingClient;
        this.gameServerId = gameServerId;
        if (matchmakingClient != null && redisClient != null && gameServerId != null) {
            this.matchmakingManager = null;
            startRedisSubscriber();
        } else {
            this.matchmakingManager = new MatchmakingManager(this);
        }
        this.disconnectionManager = new DisconnectionManager(this);
    }

    public String route(WebSocket connection, String message) {
        if (message == null || message.trim().isEmpty()) {
            return Protocol.ERROR + " empty message";
        }

        String[] parts = message.trim().split("\\s+");
        String command = parts[0].toUpperCase();

        try {
            switch (command) {
                case Protocol.REGISTER:
                    return handleRegister(parts);
                case Protocol.LOGIN:
                    return handleLogin(connection, parts);
                case Protocol.PING:
                    return Protocol.PONG;
                case Protocol.PLAY:
                    return handlePlay(connection);
                case Protocol.CANCEL_PLAY:
                    return handleCancelPlay(connection);
                case Protocol.CREATE_ROOM:
                    return handleCreateRoom(connection, parts);
                case Protocol.JOIN_ROOM:
                    return handleJoinRoom(connection, parts);
                case Protocol.LEAVE_ROOM:
                    return handleLeaveRoom(connection);
                case Protocol.LIST_ROOMS:
                    return roomManager.buildRoomListMessage();
                case Protocol.MOVE:
                    return handleMove(connection, parts);
                case Protocol.RESIGN:
                    return handleResign(connection);
                case Protocol.GAME_OVER:
                    return handleGameOver(connection, parts);
                default:
                    return Protocol.UNKNOWN_COMMAND + " " + command;
            }
        } catch (Exception e) {
            return Protocol.ERROR + " " + e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }

    public void disconnect(WebSocket connection) {
        Session session = sessionManager.markDisconnected(connection);
        if (session != null && session.isInRoom()) {
            Room room = roomManager.getRoom(session.getRoomId());
            if (room != null) {
                String color = roomManager.disconnectFromRoom(session.getRoomId(), session.getUsername());
                session.setColor(color);
                broadcastToRoom(session.getRoomId(), Protocol.OPPONENT_DISCONNECTED + " " + session.getRoomId() + " " + session.getUsername());
                disconnectionManager.startCountdown(session.getRoomId(), session.getUsername(), color);
            }
        }
        if (session == null) {
            sessionManager.removeSession(connection);
        }
    }

    public Session getSession(WebSocket connection) {
        return sessionManager.getSession(connection);
    }

    public void startMatch(String username1, String username2) {
        Session s1 = sessionManager.getSession(username1);
        Session s2 = sessionManager.getSession(username2);
        if (s1 == null || s2 == null || s1.isInRoom() || s2.isInRoom()) {
            return;
        }
        Room room = roomManager.createRoom(s1.getUsername(), "Match");
        s1.setRoomId(room.getId());
        s1.setColor("WHITE");

        String color2 = roomManager.joinRoom(room.getId(), s2.getUsername());
        s2.setRoomId(room.getId());
        s2.setColor(color2);

        send(s1.getConnection(), Protocol.JOINED_ROOM + " " + room.getId() + " WHITE");
        send(s2.getConnection(), Protocol.JOINED_ROOM + " " + room.getId() + " " + color2);
        broadcastToRoom(room.getId(), Protocol.GAME_START + " " + room.getId() + " " + username1 + " " + username2);
        eventBus.publish("game.start", "game.start", room.getId() + " " + username1 + " " + username2);
    }

    public void broadcastDisconnectCountdown(int roomId, int seconds, String username) {
        broadcastToRoom(roomId, Protocol.DISCONNECT_COUNTDOWN + " " + roomId + " " + seconds + " " + username);
    }

    public void endGameByDisconnect(int roomId, String username, String color) {
        Room room = roomManager.getRoom(roomId);
        if (room == null) {
            return;
        }
        String winner = "WHITE".equals(color) ? "BLACK" : "WHITE";
        endGame(roomId, winner, "disconnect");
        sessionManager.removeDisconnected(username);
        if (room.isEmpty()) {
            roomManager.removeRoom(roomId);
        }
    }

    public void send(WebSocket connection, String message) {
        if (connection != null && connection.isOpen()) {
            connection.send(message);
        }
    }

    public void broadcastToRoom(int roomId, String message) {
        Collection<WebSocket> connections = sessionManager.getConnectionsInRoom(roomId);
        for (WebSocket conn : connections) {
            if (conn != null && conn.isOpen()) {
                conn.send(message);
            }
        }
    }

    private void initEventBus() {
        eventBus.subscribe("game.start", e -> System.out.println("[game.start] " + e.getPayload()));
        eventBus.subscribe("game.end", e -> System.out.println("[game.end] " + e.getPayload()));
        eventBus.subscribe("move.log", e -> System.out.println("[move.log] " + e.getPayload()));
        eventBus.subscribe("score.update", e -> System.out.println("[score.update] " + e.getPayload()));
        eventBus.subscribe("sound.effect", e -> {
            String payload = e.getPayload().toString();
            int space = payload.indexOf(' ');
            if (space > 0) {
                try {
                    int roomId = Integer.parseInt(payload.substring(0, space));
                    String effect = payload.substring(space + 1);
                    broadcastToRoom(roomId, Protocol.SOUND + " " + effect);
                } catch (NumberFormatException ex) {
                    System.err.println("[sound.effect] invalid room id: " + payload);
                }
            }
        });
    }

    private void startRedisSubscriber() {
        JedisPubSub listener = new JedisPubSub() {
            @Override
            public void onMessage(String channel, String message) {
                if (channel.equals("match:found:" + gameServerId)) {
                    onMatchFound(message);
                } else if (channel.equals("match:timeout:" + gameServerId)) {
                    onMatchTimeout(message);
                }
            }
        };
        new Thread(() -> {
            try {
                redisClient.subscribe(new String[]{"match:found:" + gameServerId, "match:timeout:" + gameServerId}, listener);
            } catch (Exception e) {
                System.err.println("[CommandRouter] Redis subscriber error: " + e.getMessage());
            }
        }).start();
    }

    private void onMatchFound(String message) {
        String[] parts = message.split(" ");
        if (parts.length == 2) {
            startMatch(parts[0], parts[1]);
        }
    }

    private void onMatchTimeout(String username) {
        sendNoMatch(username);
    }

    private void sendNoMatch(String username) {
        Session session = sessionManager.getSession(username);
        if (session != null && !session.isInRoom()) {
            send(session.getConnection(), Protocol.NO_MATCH);
        }
    }

    private String handleRegister(String[] parts) {
        if (parts.length != 3) {
            return Protocol.REGISTER_FAILED + " usage: REGISTER <username> <password>";
        }
        String username = parts[1];
        String password = parts[2];
        if (authManager.register(username, password)) {
            return Protocol.REGISTER_SUCCESS + " " + username;
        }
        return Protocol.REGISTER_FAILED + " username taken or invalid";
    }

    private String handleLogin(WebSocket connection, String[] parts) {
        if (parts.length != 3) {
            return Protocol.LOGIN_FAILED + " usage: LOGIN <username> <password>";
        }
        String username = parts[1];
        String password = parts[2];
        User user = authManager.authenticate(username, password);
        if (user == null) {
            return Protocol.LOGIN_FAILED + " invalid username or password";
        }

        if (sessionManager.isDisconnected(username)) {
            Session reconnected = sessionManager.reconnect(username, connection, user);
            if (reconnected != null && reconnected.isInRoom()) {
                disconnectionManager.cancel(username);
                Room room = roomManager.getRoom(reconnected.getRoomId());
                if (room != null) {
                    String color = roomManager.rejoinRoom(room.getId(), username, reconnected.getColor());
                    reconnected.setColor(color);
                    send(connection, Protocol.JOINED_ROOM + " " + room.getId() + " " + color);
                    broadcastToRoom(room.getId(), Protocol.OPPONENT_RECONNECTED + " " + room.getId() + " " + username);
                    if (room.isGameStarted()) {
                        broadcastToRoom(room.getId(), Protocol.GAME_START + " " + room.getId() + " " + room.getWhitePlayer() + " " + room.getBlackPlayer());
                    }
                }
            }
            String token = authManager.generateToken(user);
            return Protocol.LOGIN_SUCCESS + " " + username + " " + user.getElo() + " " + token;
        }

        Session old = sessionManager.getSession(username);
        if (old != null) {
            sessionManager.removeSession(old.getConnection());
        }
        sessionManager.addSession(connection, user);
        String token = authManager.generateToken(user);
        return Protocol.LOGIN_SUCCESS + " " + username + " " + user.getElo() + " " + token;
    }

    private String handlePlay(WebSocket connection) {
        Session session = sessionManager.getSession(connection);
        if (session == null) {
            return Protocol.NOT_AUTHENTICATED;
        }
        if (session.isInRoom()) {
            return Protocol.ALREADY_IN_ROOM + " " + session.getRoomId();
        }
        if (matchmakingClient != null) {
            if (!matchmakingClient.enqueue(session.getUsername(), session.getElo(), gameServerId)) {
                return Protocol.ERROR + " matchmaking unavailable";
            }
        } else if (matchmakingManager != null) {
            matchmakingManager.enqueue(connection, session.getUsername(), session.getElo());
        }
        return null;
    }

    private String handleCancelPlay(WebSocket connection) {
        Session session = sessionManager.getSession(connection);
        if (session == null) {
            return null;
        }
        if (matchmakingClient != null) {
            matchmakingClient.cancel(session.getUsername());
        } else if (matchmakingManager != null) {
            matchmakingManager.cancel(session.getUsername());
        }
        return null;
    }

    private String handleCreateRoom(WebSocket connection, String[] parts) {
        Session session = sessionManager.getSession(connection);
        if (session == null) {
            return Protocol.NOT_AUTHENTICATED;
        }
        if (session.isInRoom()) {
            return Protocol.ALREADY_IN_ROOM + " " + session.getRoomId();
        }
        String name = parts.length > 1 ? String.join(" ", Arrays.copyOfRange(parts, 1, parts.length)) : "";
        Room room = roomManager.createRoom(session.getUsername(), name);
        session.setRoomId(room.getId());
        session.setColor("WHITE");
        return Protocol.ROOM_CREATED + " " + room.getId();
    }

    private String handleJoinRoom(WebSocket connection, String[] parts) {
        Session session = sessionManager.getSession(connection);
        if (session == null) {
            return Protocol.NOT_AUTHENTICATED;
        }
        if (parts.length < 2) {
            return Protocol.ERROR + " usage: JOIN_ROOM <roomId>";
        }
        int roomId;
        try {
            roomId = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            return Protocol.ERROR + " invalid room id";
        }
        if (session.isInRoom()) {
            return Protocol.ALREADY_IN_ROOM + " " + session.getRoomId();
        }

        String color = roomManager.joinRoom(roomId, session.getUsername());
        if (color == null) {
            return Protocol.ROOM_NOT_FOUND;
        }

        session.setRoomId(roomId);
        session.setColor(color);
        Room room = roomManager.getRoom(roomId);
        String joinedMessage = Protocol.JOINED_ROOM + " " + roomId + " " + color;
        send(connection, joinedMessage);

        String broadcast = Protocol.ROOM_UPDATED + " " + roomId + " " + session.getUsername() + " " + color;
        broadcastToRoom(roomId, broadcast);

        if (room != null && room.isGameStarted()) {
            broadcastToRoom(roomId, Protocol.GAME_START + " " + roomId + " " + room.getWhitePlayer() + " " + room.getBlackPlayer());
        }
        return joinedMessage;
    }

    private String handleLeaveRoom(WebSocket connection) {
        Session session = sessionManager.getSession(connection);
        if (session == null) {
            return Protocol.NOT_AUTHENTICATED;
        }
        if (!session.isInRoom()) {
            return Protocol.NOT_IN_ROOM;
        }
        int roomId = session.getRoomId();
        String username = session.getUsername();
        Room room = roomManager.getRoom(roomId);
        if (room == null) {
            session.leaveRoom();
            session.setColor(null);
            return Protocol.NOT_IN_ROOM;
        }
        boolean closed = room.isCreator(username);
        roomManager.leaveRoom(roomId, username);
        session.leaveRoom();
        session.setColor(null);

        if (closed) {
            broadcastToRoom(roomId, Protocol.ROOM_CLOSED + " " + roomId + " " + username);
        } else {
            broadcastToRoom(roomId, Protocol.LEFT_ROOM + " " + roomId + " " + username);
        }
        return Protocol.LEFT_ROOM + " " + roomId;
    }

    private String handleMove(WebSocket connection, String[] parts) {
        Session session = sessionManager.getSession(connection);
        if (session == null) {
            return Protocol.NOT_AUTHENTICATED;
        }
        if (!session.isInRoom()) {
            return Protocol.NOT_IN_ROOM;
        }
        if (parts.length < 5) {
            return Protocol.ERROR + " usage: MOVE <fromRow> <fromCol> <toRow> <toCol> [capture]";
        }
        int roomId = session.getRoomId();
        Room room = roomManager.getRoom(roomId);
        if (room == null || room.getGameState() == null) {
            return Protocol.ERROR + " game not started";
        }
        GameState gameState = room.getGameState();
        if (gameState.isGameOver()) {
            return Protocol.ERROR + " game over";
        }

        Position from;
        Position to;
        try {
            from = new Position(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
            to = new Position(Integer.parseInt(parts[3]), Integer.parseInt(parts[4]));
        } catch (NumberFormatException e) {
            return Protocol.ERROR + " invalid positions";
        }

        String color = room.getColor(session.getUsername());
        if (color == null || "VIEWER".equals(color)) {
            return Protocol.ERROR + " not a player";
        }

        GameState.MoveResult result = gameState.applyMove(color, from, to);
        if (!result.success) {
            return Protocol.ERROR + " " + result.error;
        }

        String moveMessage = Protocol.MOVE + " " + roomId + " " + from.getRow() + " " + from.getCol() + " " + to.getRow() + " " + to.getCol();
        String soundEffect = result.capture ? "eat" : "move";
        broadcastToRoom(roomId, moveMessage);
        eventBus.publish("move.log", "move.log", session.getUsername() + " " + from.getRow() + " " + from.getCol() + " " + to.getRow() + " " + to.getCol());
        eventBus.publish("sound.effect", "sound.effect", roomId + " " + soundEffect);

        if (result.promotion) {
            broadcastToRoom(roomId, Protocol.PROMOTION + " " + roomId + " " + to.getRow() + " " + to.getCol() + " Q");
        }

        if (result.gameOver) {
            String reason = "DRAW".equals(result.winner) ? "stalemate" : "gameover";
            endGame(roomId, result.winner, reason);
        }

        return null;
    }

    private String handleResign(WebSocket connection) {
        Session session = sessionManager.getSession(connection);
        if (session == null) {
            return Protocol.NOT_AUTHENTICATED;
        }
        if (!session.isInRoom()) {
            return Protocol.NOT_IN_ROOM;
        }
        Room room = roomManager.getRoom(session.getRoomId());
        if (room == null) {
            return Protocol.NOT_IN_ROOM;
        }
        String color = room.getColor(session.getUsername());
        if (color == null || "VIEWER".equals(color)) {
            return Protocol.ERROR + " not a player";
        }
        String winner = "WHITE".equals(color) ? "BLACK" : "WHITE";
        endGame(session.getRoomId(), winner, "resign");
        return null;
    }

    private String handleGameOver(WebSocket connection, String[] parts) {
        if (parts.length < 2) {
            return Protocol.ERROR + " usage: GAME_OVER <WHITE|BLACK|DRAW>";
        }
        String result = parts[1].toUpperCase();
        if (!"WHITE".equals(result) && !"BLACK".equals(result) && !"DRAW".equals(result)) {
            return Protocol.ERROR + " invalid result";
        }
        Session session = sessionManager.getSession(connection);
        if (session == null) {
            return Protocol.NOT_AUTHENTICATED;
        }
        if (!session.isInRoom()) {
            return Protocol.NOT_IN_ROOM;
        }
        Room room = roomManager.getRoom(session.getRoomId());
        if (room == null) {
            return Protocol.NOT_IN_ROOM;
        }
        String color = room.getColor(session.getUsername());
        if (color == null || "VIEWER".equals(color)) {
            return Protocol.ERROR + " not a player";
        }
        if (!"DRAW".equals(result) && !result.equals(color)) {
            return Protocol.ERROR + " cannot declare opponent as winner";
        }
        endGame(session.getRoomId(), result, "gameover");
        return null;
    }

    private void endGame(int roomId, String result, String reason) {
        Room room = roomManager.getRoom(roomId);
        if (room == null) {
            return;
        }
        String white = room.getWhitePlayer();
        String black = room.getBlackPlayer();
        if (white == null || black == null) {
            return;
        }
        if (room.isGameOver()) {
            return;
        }
        eloManager.updateRatings(white, black, result);
        room.endGame(result);

        User whiteUser = authManager.getUser(white);
        User blackUser = authManager.getUser(black);
        broadcastToRoom(roomId, Protocol.GAME_OVER + " " + result);
        eventBus.publish("game.end", "game.end", roomId + " " + result + " " + reason);
        if (whiteUser != null && blackUser != null) {
            broadcastToRoom(roomId, Protocol.ELO_UPDATE + " " + white + " " + whiteUser.getElo());
            broadcastToRoom(roomId, Protocol.ELO_UPDATE + " " + black + " " + blackUser.getElo());
            eventBus.publish("score.update", "score.update", white + "=" + whiteUser.getElo() + " " + black + "=" + blackUser.getElo());
        }
    }
}
