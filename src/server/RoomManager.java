package server;

import network.Protocol;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class RoomManager {
    private final Map<Integer, Room> rooms = new ConcurrentHashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(1);

    public synchronized Room createRoom(String creator, String name) {
        int id = nextId.getAndIncrement();
        Room room = new Room(id, name, creator);
        rooms.put(id, room);
        return room;
    }

    public Room getRoom(int id) {
        return rooms.get(id);
    }

    public boolean roomExists(int id) {
        return rooms.containsKey(id);
    }

    public synchronized String joinRoom(int id, String username) {
        Room room = rooms.get(id);
        if (room == null) {
            return null;
        }
        if (room.hasUser(username)) {
            return room.getColor(username);
        }
        if (room.isFull()) {
            return "VIEWER";
        }
        return room.addUser(username);
    }

    public synchronized String disconnectFromRoom(int id, String username) {
        Room room = rooms.get(id);
        if (room == null) {
            return null;
        }
        String color = room.getColor(username);
        room.disconnectUser(username);
        return color;
    }

    public synchronized String rejoinRoom(int id, String username, String preferredColor) {
        Room room = rooms.get(id);
        if (room == null) {
            return null;
        }
        return room.rejoinUser(username, preferredColor);
    }

    public synchronized boolean leaveRoom(int id, String username) {
        Room room = rooms.get(id);
        if (room == null) {
            return false;
        }
        if (room.isCreator(username)) {
            rooms.remove(id);
            return true;
        }
        room.removeUser(username);
        if (room.isEmpty()) {
            rooms.remove(id);
        }
        return true;
    }

    public synchronized void removeRoom(int id) {
        rooms.remove(id);
    }

    public List<Room> listRooms() {
        return new ArrayList<>(rooms.values());
    }

    public String buildRoomListMessage() {
        List<Room> list = listRooms();
        if (list.isEmpty()) {
            return Protocol.ROOM_LIST;
        }
        StringBuilder sb = new StringBuilder(Protocol.ROOM_LIST);
        for (Room room : list) {
            sb.append(" ").append(room.toSummaryString());
        }
        return sb.toString();
    }
}
