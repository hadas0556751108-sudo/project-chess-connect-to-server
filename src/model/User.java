package model;

public class User {
    private final String username;
    private final String passwordHash;
    private int elo;

    public User(String username, String passwordHash, int elo) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.elo = elo;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public int getElo() {
        return elo;
    }

    public void setElo(int elo) {
        this.elo = elo;
    }
}
