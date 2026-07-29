package server;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Room {
    private final int id;
    private final String name;
    private final String creator;
    private String whitePlayer;
    private String blackPlayer;
    private final List<String> viewers = new ArrayList<>();
    private String currentColor = "WHITE";
    private boolean gameStarted = false;
    private boolean gameOver = false;
    private GameState gameState;

    public Room(int id, String name, String creator) {
        this.id = id;
        this.name = (name == null || name.isEmpty()) ? "Room " + id : name;
        this.creator = creator;
        this.whitePlayer = creator;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCreator() {
        return creator;
    }

    public synchronized boolean hasUser(String username) {
        if (whitePlayer != null && whitePlayer.equals(username)) return true;
        if (blackPlayer != null && blackPlayer.equals(username)) return true;
        return viewers.contains(username);
    }

    public synchronized String addUser(String username) {
        if (whitePlayer == null) {
            whitePlayer = username;
            startIfReady();
            return "WHITE";
        }
        if (blackPlayer == null && !whitePlayer.equals(username)) {
            blackPlayer = username;
            startIfReady();
            return "BLACK";
        }
        if (!viewers.contains(username) && !username.equals(whitePlayer) && !username.equals(blackPlayer)) {
            viewers.add(username);
        }
        return "VIEWER";
    }

    public synchronized String getColor(String username) {
        if (whitePlayer != null && whitePlayer.equals(username)) return "WHITE";
        if (blackPlayer != null && blackPlayer.equals(username)) return "BLACK";
        if (viewers.contains(username)) return "VIEWER";
        return null;
    }

    public synchronized boolean isColorAvailable(String color) {
        if ("WHITE".equals(color)) return whitePlayer == null;
        if ("BLACK".equals(color)) return blackPlayer == null;
        return false;
    }

    public synchronized String rejoinUser(String username, String preferredColor) {
        if ("WHITE".equals(preferredColor) && whitePlayer == null) {
            whitePlayer = username;
            startIfReady();
            return "WHITE";
        }
        if ("BLACK".equals(preferredColor) && blackPlayer == null) {
            blackPlayer = username;
            startIfReady();
            return "BLACK";
        }
        return addUser(username);
    }

    public synchronized void removeUser(String username) {
        if (whitePlayer != null && whitePlayer.equals(username)) {
            whitePlayer = null;
        }
        if (blackPlayer != null && blackPlayer.equals(username)) {
            blackPlayer = null;
        }
        viewers.remove(username);
        if (whitePlayer == null || blackPlayer == null) {
            gameStarted = false;
        }
        if (whitePlayer != null && blackPlayer == null) {
            currentColor = "WHITE";
        }
    }

    public synchronized void disconnectUser(String username) {
        if (whitePlayer != null && whitePlayer.equals(username)) {
            whitePlayer = null;
        }
        if (blackPlayer != null && blackPlayer.equals(username)) {
            blackPlayer = null;
        }
        viewers.remove(username);
        gameStarted = false;
    }

    public synchronized boolean isCreator(String username) {
        return creator.equals(username);
    }

    public synchronized boolean isFull() {
        return whitePlayer != null && blackPlayer != null;
    }

    public synchronized boolean isEmpty() {
        return whitePlayer == null && blackPlayer == null && viewers.isEmpty();
    }

    public synchronized boolean isGameStarted() {
        return gameStarted;
    }

    public synchronized boolean isGameOver() {
        return gameOver;
    }

    public synchronized GameState getGameState() {
        return gameState;
    }

    public synchronized void endGame() {
        endGame(null);
    }

    public synchronized void endGame(String winner) {
        gameStarted = false;
        gameOver = true;
        currentColor = "WHITE";
        if (gameState != null) {
            gameState.endGame(winner);
        }
    }

    public synchronized String getCurrentColor() {
        return currentColor;
    }

    public synchronized void nextTurn() {
        currentColor = "WHITE".equals(currentColor) ? "BLACK" : "WHITE";
    }

    public synchronized List<String> getParticipants() {
        List<String> list = new ArrayList<>();
        if (whitePlayer != null) list.add(whitePlayer);
        if (blackPlayer != null) list.add(blackPlayer);
        list.addAll(viewers);
        return Collections.unmodifiableList(list);
    }

    public synchronized String getStatus() {
        if (gameStarted) return "PLAYING";
        if (blackPlayer == null) return "WAITING";
        return "WAITING";
    }

    public synchronized String toSummaryString() {
        StringBuilder sb = new StringBuilder();
        sb.append(id).append(":").append(name).append(":").append(getStatus()).append(":");
        sb.append(whitePlayer == null ? "-" : whitePlayer);
        sb.append("/");
        sb.append(blackPlayer == null ? "-" : blackPlayer);
        if (!viewers.isEmpty()) sb.append(" (+").append(viewers.size()).append(" viewers)");
        return sb.toString();
    }

    public synchronized String getWhitePlayer() {
        return whitePlayer;
    }

    public synchronized String getBlackPlayer() {
        return blackPlayer;
    }

    private void startIfReady() {
        if (!gameOver && whitePlayer != null && blackPlayer != null && !gameStarted) {
            gameStarted = true;
            currentColor = "WHITE";
            gameState = new GameState();
        }
    }
}
