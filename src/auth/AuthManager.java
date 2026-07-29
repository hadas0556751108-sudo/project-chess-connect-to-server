package auth;

import at.favre.lib.crypto.bcrypt.BCrypt;
import database.UserRepository;
import model.User;

public class AuthManager {
    private final UserRepository repository;
    private final JwtUtil jwtUtil;

    public AuthManager(UserRepository repository) {
        this(repository, System.getenv().getOrDefault("JWT_SECRET", "dev-secret-do-not-use-in-production"));
    }

    public AuthManager(UserRepository repository, String jwtSecret) {
        this.repository = repository;
        this.jwtUtil = new JwtUtil(jwtSecret);
    }

    public boolean register(String username, String password) {
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            return false;
        }
        if (repository.exists(username)) {
            return false;
        }
        String hash = BCrypt.withDefaults().hashToString(12, password.toCharArray());
        User user = new User(username, hash, 1200);
        return repository.createUser(user);
    }

    public User authenticate(String username, String password) {
        if (username == null || password == null) {
            return null;
        }
        User user = repository.findByUsername(username);
        if (user == null) {
            return null;
        }
        if (BCrypt.verifyer().verify(password.toCharArray(), user.getPasswordHash()).verified) {
            return user;
        }
        return null;
    }

    public String generateToken(User user) {
        return jwtUtil.createToken(user.getUsername(), user.getElo());
    }

    public User validateToken(String token) {
        return jwtUtil.verifyToken(token);
    }

    public User getUser(String username) {
        return repository.findByUsername(username);
    }

    public boolean updateElo(String username, int elo) {
        return repository.updateElo(username, elo);
    }
}
