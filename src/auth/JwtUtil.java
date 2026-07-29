package auth;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import model.User;

import java.util.Date;

public class JwtUtil {
    private final Algorithm algorithm;
    private final long ttlMillis = 24 * 60 * 60 * 1000; // 24 hours

    public JwtUtil(String secret) {
        this.algorithm = Algorithm.HMAC256(secret);
    }

    public String createToken(String username, int elo) {
        return JWT.create()
                .withSubject(username)
                .withClaim("elo", elo)
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + ttlMillis))
                .sign(algorithm);
    }

    public User verifyToken(String token) {
        try {
            DecodedJWT jwt = JWT.require(algorithm)
                    .build()
                    .verify(token);
            String username = jwt.getSubject();
            int elo = jwt.getClaim("elo").asInt();
            return new User(username, "", elo);
        } catch (JWTVerificationException e) {
            return null;
        }
    }
}
