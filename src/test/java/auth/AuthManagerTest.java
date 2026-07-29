package auth;

import at.favre.lib.crypto.bcrypt.BCrypt;
import database.UserRepository;
import model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthManagerTest {

    @Mock
    private UserRepository repository;

    private AuthManager authManager;

    @BeforeEach
    void setUp() {
        authManager = new AuthManager(repository, "test-secret");
    }

    @Test
    void registerNewUser() {
        when(repository.exists("alice")).thenReturn(false);
        when(repository.createUser(any(User.class))).thenReturn(true);

        assertTrue(authManager.register("alice", "password123"));
        verify(repository).createUser(any(User.class));
    }

    @Test
    void registerDuplicateUser() {
        when(repository.exists("alice")).thenReturn(true);
        assertFalse(authManager.register("alice", "password123"));
        verify(repository, never()).createUser(any(User.class));
    }

    @Test
    void registerInvalidInput() {
        assertFalse(authManager.register("", "password"));
        assertFalse(authManager.register("alice", ""));
        assertFalse(authManager.register(null, "password"));
    }

    @Test
    void authenticateSuccess() {
        String hash = BCrypt.withDefaults().hashToString(12, "password123".toCharArray());
        when(repository.findByUsername("alice")).thenReturn(new User("alice", hash, 1200));

        User user = authManager.authenticate("alice", "password123");
        assertNotNull(user);
        assertEquals("alice", user.getUsername());
    }

    @Test
    void authenticateWrongPassword() {
        String hash = BCrypt.withDefaults().hashToString(12, "password123".toCharArray());
        when(repository.findByUsername("alice")).thenReturn(new User("alice", hash, 1200));

        assertNull(authManager.authenticate("alice", "wrong"));
    }

    @Test
    void authenticateUnknownUser() {
        when(repository.findByUsername("bob")).thenReturn(null);
        assertNull(authManager.authenticate("bob", "password"));
    }

    @Test
    void tokenRoundTrip() {
        when(repository.findByUsername("alice")).thenReturn(new User("alice", "hash", 1500));

        User user = authManager.getUser("alice");
        String token = authManager.generateToken(user);
        assertNotNull(token);

        User verified = authManager.validateToken(token);
        assertNotNull(verified);
        assertEquals("alice", verified.getUsername());
        assertEquals(1500, verified.getElo());
    }

    @Test
    void updateElo() {
        when(repository.updateElo("alice", 1300)).thenReturn(true);
        assertTrue(authManager.updateElo("alice", 1300));
        verify(repository).updateElo("alice", 1300);
    }

    @Test
    void getUserNullWhenNotFound() {
        when(repository.findByUsername("missing")).thenReturn(null);
        assertNull(authManager.getUser("missing"));
    }
}
