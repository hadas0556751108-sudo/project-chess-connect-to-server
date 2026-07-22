package network;

public final class Protocol {
    private Protocol() {}

    public static final String SEPARATOR = " ";

    // Client -> Server
    public static final String REGISTER = "REGISTER";
    public static final String LOGIN = "LOGIN";
    public static final String PLAY = "PLAY";
    public static final String CANCEL_PLAY = "CANCEL_PLAY";
    public static final String CREATE_ROOM = "CREATE_ROOM";
    public static final String JOIN_ROOM = "JOIN_ROOM";
    public static final String LEAVE_ROOM = "LEAVE_ROOM";
    public static final String LIST_ROOMS = "LIST_ROOMS";
    public static final String MOVE = "MOVE";
    public static final String RESIGN = "RESIGN";
    public static final String PING = "PING";

    // Server -> Client
    public static final String REGISTER_SUCCESS = "REGISTER_SUCCESS";
    public static final String REGISTER_FAILED = "REGISTER_FAILED";
    public static final String LOGIN_SUCCESS = "LOGIN_SUCCESS";
    public static final String LOGIN_FAILED = "LOGIN_FAILED";
    public static final String NO_MATCH = "NO_MATCH";
    public static final String GAME_START = "GAME_START";
    public static final String GAME_OVER = "GAME_OVER";
    public static final String ELO_UPDATE = "ELO_UPDATE";
    public static final String SOUND = "SOUND";
    public static final String DRAW = "DRAW";
    public static final String OPPONENT_DISCONNECTED = "OPPONENT_DISCONNECTED";
    public static final String OPPONENT_RECONNECTED = "OPPONENT_RECONNECTED";
    public static final String DISCONNECT_COUNTDOWN = "DISCONNECT_COUNTDOWN";
    public static final String ROOM_CREATED = "ROOM_CREATED";
    public static final String JOINED_ROOM = "JOINED_ROOM";
    public static final String LEFT_ROOM = "LEFT_ROOM";
    public static final String ROOM_CLOSED = "ROOM_CLOSED";
    public static final String ROOM_LIST = "ROOM_LIST";
    public static final String ROOM_UPDATED = "ROOM_UPDATED";
    public static final String NOT_AUTHENTICATED = "NOT_AUTHENTICATED";
    public static final String NOT_IN_ROOM = "NOT_IN_ROOM";
    public static final String ALREADY_IN_ROOM = "ALREADY_IN_ROOM";
    public static final String ROOM_NOT_FOUND = "ROOM_NOT_FOUND";
    public static final String ROOM_FULL = "ROOM_FULL";
    public static final String NOT_YOUR_TURN = "NOT_YOUR_TURN";
    public static final String UNKNOWN_COMMAND = "UNKNOWN_COMMAND";
    public static final String ERROR = "ERROR";
    public static final String PONG = "PONG";
}
