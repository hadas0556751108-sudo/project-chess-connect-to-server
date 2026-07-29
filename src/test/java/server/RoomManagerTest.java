package server;

import network.Protocol;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RoomManagerTest {

    @Test
    void createRoomAndJoin() {
        RoomManager manager = new RoomManager();
        Room room = manager.createRoom("alice", "MyRoom");
        assertNotNull(room);
        assertEquals("alice", room.getCreator());
        assertEquals("BLACK", manager.joinRoom(room.getId(), "bob"));
        assertEquals("VIEWER", manager.joinRoom(room.getId(), "carol"));
    }

    @Test
    void joinUnknownRoomReturnsNull() {
        RoomManager manager = new RoomManager();
        assertNull(manager.joinRoom(999, "bob"));
    }

    @Test
    void rejoinAndDisconnect() {
        RoomManager manager = new RoomManager();
        Room room = manager.createRoom("alice", "MyRoom");
        manager.joinRoom(room.getId(), "bob");
        String color = manager.disconnectFromRoom(room.getId(), "bob");
        assertEquals("BLACK", color);
        assertEquals("BLACK", manager.rejoinRoom(room.getId(), "bob", "BLACK"));
    }

    @Test
    void leaveRoomRemovesNonCreator() {
        RoomManager manager = new RoomManager();
        Room room = manager.createRoom("alice", "MyRoom");
        manager.joinRoom(room.getId(), "bob");
        assertTrue(manager.leaveRoom(room.getId(), "bob"));
        assertTrue(manager.roomExists(room.getId()));
    }

    @Test
    void creatorLeaveRemovesRoom() {
        RoomManager manager = new RoomManager();
        Room room = manager.createRoom("alice", "MyRoom");
        manager.joinRoom(room.getId(), "bob");
        assertTrue(manager.leaveRoom(room.getId(), "alice"));
        assertFalse(manager.roomExists(room.getId()));
    }

    @Test
    void leaveUnknownRoomReturnsFalse() {
        RoomManager manager = new RoomManager();
        assertFalse(manager.leaveRoom(999, "bob"));
    }

    @Test
    void listRoomsMessage() {
        RoomManager manager = new RoomManager();
        Room room = manager.createRoom("alice", "MyRoom");
        manager.joinRoom(room.getId(), "bob");
        String list = manager.buildRoomListMessage();
        assertTrue(list.startsWith(Protocol.ROOM_LIST));
        assertTrue(list.contains("MyRoom"));
    }

    @Test
    void emptyListMessage() {
        RoomManager manager = new RoomManager();
        assertEquals(Protocol.ROOM_LIST, manager.buildRoomListMessage());
    }

    @Test
    void removeRoom() {
        RoomManager manager = new RoomManager();
        Room room = manager.createRoom("alice", "MyRoom");
        manager.removeRoom(room.getId());
        assertNull(manager.getRoom(room.getId()));
    }
}
