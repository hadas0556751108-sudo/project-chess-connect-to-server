package engine;

import model.Position;

import java.util.ArrayList;
import java.util.List;

public class GameSnapshot {
    private final int boardWidth;
    private final int boardHeight;
    private final List<PieceSnapshot> pieces;
    private final Position selectedPosition;
    private final boolean gameOver;
    private final String winner;

    public GameSnapshot(int boardWidth, int boardHeight, List<PieceSnapshot> pieces,
                       Position selectedPosition, boolean gameOver, String winner) {
        this.boardWidth = boardWidth;
        this.boardHeight = boardHeight;
        this.pieces = new ArrayList<>(pieces);
        this.selectedPosition = selectedPosition;
        this.gameOver = gameOver;
        this.winner = winner;
    }

    public int boardWidth() { return boardWidth; }
    public int boardHeight() { return boardHeight; }
    public List<PieceSnapshot> pieces() { return pieces; }
    public Position selectedPosition() { return selectedPosition; }
    public boolean gameOver() { return gameOver; }
    public String winner() { return winner; }
}
