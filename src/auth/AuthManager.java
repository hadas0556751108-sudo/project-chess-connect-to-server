package auth;

import database.UserRepository;
import model.User;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Base64;

public class AuthManager {
    private final UserRepository repository;

    public AuthManager(UserRepository repository) {
        this.repository = repository;
    }

    public boolean register(String username, String password) throws SQLException {
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            return false;
        }
        if (repository.exists(username)) {
            return false;
        }
        String hash = hashPassword(username, password);
        User user = new User(username, hash, 1200);
        return repository.createUser(user);
    }

    public User authenticate(String username, String password) throws SQLException {
        if (username == null || password == null) {
            return null;
        }
        User user = repository.findByUsername(username);
        if (user == null) {
            return null;
        }
        String hash = hashPassword(username, password);
        if (hash.equals(user.getPasswordHash())) {
            return user;
        }
        return null;
    }

    public User getUser(String username) throws SQLException {
        return repository.findByUsername(username);
    }

    public boolean updateElo(String username, int elo) throws SQLException {
        return repository.updateElo(username, elo);
    }

    private String hashPassword(String username, String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String input = password + ":" + username;
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
