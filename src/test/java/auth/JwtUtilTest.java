package auth;

import model.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private final JwtUtil jwtUtil = new JwtUtil("test-secret");

    @Test
    void createAndVerifyToken() {
        String token = jwtUtil.createToken("alice", 1500);
        assertNotNull(token);
        assertFalse(token.isEmpty());

        User user = jwtUtil.verifyToken(token);
        assertNotNull(user);
        assertEquals("alice", user.getUsername());
        assertEquals(1500, user.getElo());
    }

    @Test
    void verifyInvalidToken() {
        assertNull(jwtUtil.verifyToken("not.a.token"));
    }

    @Test
    void verifyTamperedToken() {
        String token = jwtUtil.createToken("alice", 1500);
        assertNull(jwtUtil.verifyToken(token + "x"));
    }

    @Test
    void verifyNullToken() {
        assertNull(jwtUtil.verifyToken(null));
    }
}
