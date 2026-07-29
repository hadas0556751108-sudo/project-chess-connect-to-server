package server;

import auth.AuthManager;
import model.User;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class EloManagerTest {

    @Test
    void whiteWins() {
        AuthManager auth = mock(AuthManager.class);
        when(auth.getUser("white")).thenReturn(new User("white", "", 1500));
        when(auth.getUser("black")).thenReturn(new User("black", "", 1300));

        new EloManager(auth).updateRatings("white", "black", "WHITE");

        ArgumentCaptor<Integer> whiteElo = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> blackElo = ArgumentCaptor.forClass(Integer.class);
        verify(auth).updateElo(eq("white"), whiteElo.capture());
        verify(auth).updateElo(eq("black"), blackElo.capture());

        assertEquals(1508, whiteElo.getValue());
        assertEquals(1292, blackElo.getValue());
    }

    @Test
    void blackWins() {
        AuthManager auth = mock(AuthManager.class);
        when(auth.getUser("white")).thenReturn(new User("white", "", 1500));
        when(auth.getUser("black")).thenReturn(new User("black", "", 1300));

        new EloManager(auth).updateRatings("white", "black", "BLACK");

        ArgumentCaptor<Integer> whiteElo = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> blackElo = ArgumentCaptor.forClass(Integer.class);
        verify(auth).updateElo(eq("white"), whiteElo.capture());
        verify(auth).updateElo(eq("black"), blackElo.capture());

        assertEquals(1476, whiteElo.getValue());
        assertEquals(1324, blackElo.getValue());
    }

    @Test
    void draw() {
        AuthManager auth = mock(AuthManager.class);
        when(auth.getUser("white")).thenReturn(new User("white", "", 1500));
        when(auth.getUser("black")).thenReturn(new User("black", "", 1500));

        new EloManager(auth).updateRatings("white", "black", "DRAW");

        verify(auth).updateElo("white", 1500);
        verify(auth).updateElo("black", 1500);
    }

    @Test
    void missingUsersDoesNothing() {
        AuthManager auth = mock(AuthManager.class);
        new EloManager(auth).updateRatings("white", "black", "WHITE");
        verify(auth, never()).updateElo(anyString(), anyInt());
    }

    @Test
    void invalidResultDoesNothing() {
        AuthManager auth = mock(AuthManager.class);
        when(auth.getUser("white")).thenReturn(new User("white", "", 1500));
        when(auth.getUser("black")).thenReturn(new User("black", "", 1300));

        new EloManager(auth).updateRatings("white", "black", "UNKNOWN");

        verify(auth, never()).updateElo(anyString(), anyInt());
    }
}
