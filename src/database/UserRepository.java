package database;

import model.User;

import java.sql.*;

public class UserRepository {
    private final String dbPath;

    public UserRepository(String dbPath) {
        this.dbPath = dbPath;
    }

    public void init() throws SQLException {
        String url = "jdbc:sqlite:" + dbPath;
        try (Connection conn = DriverManager.getConnection(url)) {
            if (conn != null) {
                Statement stmt = conn.createStatement();
                stmt.execute("CREATE TABLE IF NOT EXISTS users ("
                        + " id INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + " username TEXT UNIQUE NOT NULL,"
                        + " password_hash TEXT NOT NULL,"
                        + " elo INTEGER NOT NULL DEFAULT 1200"
                        + ")");
                stmt.close();
            }
        }
    }

    public boolean createUser(User user) throws SQLException {
        String url = "jdbc:sqlite:" + dbPath;
        String sql = "INSERT INTO users(username, password_hash, elo) VALUES(?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPasswordHash());
            pstmt.setInt(3, user.getElo());
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("UNIQUE constraint failed")) {
                return false;
            }
            throw e;
        }
    }

    public User findByUsername(String username) throws SQLException {
        String url = "jdbc:sqlite:" + dbPath;
        String sql = "SELECT username, password_hash, elo FROM users WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new User(
                            rs.getString("username"),
                            rs.getString("password_hash"),
                            rs.getInt("elo")
                    );
                }
            }
        }
        return null;
    }

    public boolean updateElo(String username, int elo) throws SQLException {
        String url = "jdbc:sqlite:" + dbPath;
        String sql = "UPDATE users SET elo = ? WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(url);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, elo);
            pstmt.setString(2, username);
            int rows = pstmt.executeUpdate();
            return rows > 0;
        }
    }

    public boolean exists(String username) throws SQLException {
        return findByUsername(username) != null;
    }
}
