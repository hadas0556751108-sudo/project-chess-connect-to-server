package server;

import org.junit.jupiter.api.Test;

import java.util.List;

import static com.mongodb.internal.connection.tlschannel.util.Util.assertTrue;
import static org.junit.jupiter.api.Assertions.*;

class RoomTest {

    @Test
    void creatorStartsAsWhite() {
        Room room = new Room(1, "Test", "alice");
        assertEquals("WHITE", room.getColor("alice"));
        assertEquals("alice", room.getWhitePlayer());
        assertTrue(room.isCreator("alice"));
    }

    @Test
    void addSecondPlayerBecomesBlack() {
        Room room = new Room(1, "Test", "alice");
        assertEquals("BLACK", room.addUser("bob"));
        assertEquals("bob", room.getBlackPlayer());
        assertTrue(room.isFull());
        assertTrue(room.isGameStarted());
    }

    @Test
    void thirdUserBecomesViewer() {
        Room room = new Room(1, "Test", "alice");
        room.addUser("bob");
        assertEquals("VIEWER", room.addUser("carol"));
        assertTrue(room.getParticipants().contains("carol"));
    }

    @Test
    void cannotTakeTakenColor() {
        Room room = new Room(1, "Test", "alice");
        assertFalse(room.isColorAvailable("WHITE"));
    }

    @Test
    void rejoinWithPreferredColor() {
        Room room = new Room(1, "Test", "alice");
        room.addUser("bob");
        room.disconnectUser("bob");
        assertEquals("BLACK", room.rejoinUser("bob", "BLACK"));
    }

    @Test
    void removePlayerStopsGame() {
        Room room = new Room(1, "Test", "alice");
        room.addUser("bob");
        assertTrue(room.isGameStarted());
        room.removeUser("bob");
        assertFalse(room.isGameStarted());
    }

    @Test
    void creatorLeavesButRoomStays() {
        Room room = new Room(1, "Test", "alice");
        room.addUser("bob");
        room.removeUser("alice");
        assertNull(room.getWhitePlayer());
        assertEquals("bob", room.getBlackPlayer());
    }

    @Test
    void nextTurnTogglesColor() {
        Room room = new Room(1, "Test", "alice");
        assertEquals("WHITE", room.getCurrentColor());
        room.nextTurn();
        assertEquals("BLACK", room.getCurrentColor());
        room.nextTurn();
        assertEquals("WHITE", room.getCurrentColor());
    }

    @Test
    void gameOverEndsRoom() {
        Room room = new Room(1, "Test", "alice");
        room.addUser("bob");
        room.endGame();
        assertTrue(room.isGameOver());
        assertFalse(room.isGameStarted());
    }

    @Test
    void emptyWhenNoPlayers() {
        Room room = new Room(1, "Test", "alice");
        room.removeUser("alice");
        assertTrue(room.isEmpty());
    }

    @Test
    void summaryContainsStatusAndPlayers() {
        Room room = new Room(1, "Test", "alice");
        room.addUser("bob");
        String summary = room.toSummaryString();
        assertTrue(summary.contains("alice"));
        assertTrue(summary.contains("bob"));
    }

    @Test
    void participantsAreUnmodifiable() {
        Room room = new Room(1, "Test", "alice");
        List<String> participants = room.getParticipants();
        assertThrows(UnsupportedOperationException.class, () -> participants.add("eve"));
    }
}
