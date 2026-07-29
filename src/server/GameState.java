package server;

import engine.GameEngine;
import engine.RealTimeManager;
import engine.StateConfigLoader;
import model.Board;
import model.Piece;
import model.Position;
import network.Protocol;

import java.util.ArrayList;
import java.util.List;

/**
 * Authoritative server-side chess state. Wraps the client GameEngine
 * so the server validates and applies moves using the exact same rules.
 */
public class GameState {

    private final GameEngine engine;
    private final RealTimeManager realTimeManager;
    private boolean gameOver = false;
    private String winner = null;
    private final List<String> moveHistory = new ArrayList<>();

    public GameState() {
        Board board = createInitialBoard();
        this.realTimeManager = new RealTimeManager();
        this.engine = new GameEngine(board, this.realTimeManager, new StateConfigLoader("assets/pieces_mine", 100.0));
    }

    public Board getBoard() {
        return engine.getBoard();
    }

    public char getCurrentTurn() {
        return engine.getCurrentTurn();
    }

    public boolean isGameOver() {
        return gameOver || engine.isGameOver();
    }

    public String getWinner() {
        if (winner != null) {
            return winner;
        }
        return mapWinner(engine.getWinner());
    }

    public List<String> getMoveHistory() {
        return moveHistory;
    }

    public void endGame(String winner) {
        this.gameOver = true;
        this.winner = winner;
    }

    public MoveResult applyMove(String color, Position from, Position to) {
        if (color == null) {
            return fail("missing color");
        }
        char c = colorChar(color);
        if (c == 0) {
            return fail("invalid color");
        }
        return applyMove(c, from, to);
    }

    public MoveResult applyMove(char playerColor, Position from, Position to) {
        if (isGameOver()) {
            return fail("game over");
        }
        if (from == null || to == null) {
            return fail("missing positions");
        }

        Board board = engine.getBoard();
        Piece moving = board.getPieceAt(from);
        if (moving == null) {
            return fail("no piece at source");
        }
        if (moving.getColor() != playerColor) {
            return fail("not your piece");
        }

        Piece target = board.getPieceAt(to);
        char movingType = moving.getType();

        engine.executeMove(from, to);

        if (!realTimeManager.hasPendingMoves()) {
            return fail("illegal move");
        }

        realTimeManager.advanceClock(60_000, board, engine);

        board = engine.getBoard();
        Piece placed = board.getPieceAt(to);
        boolean capture = target != null && target.getColor() != playerColor;
        boolean promotion = (movingType == 'P' && placed != null && placed.getType() == 'Q' && placed.getColor() == playerColor);

        moveHistory.add(formatMove(playerColor, from, to, placed != null ? placed : moving, capture));

        String resultWinner = getWinner();
        boolean isGameOver = isGameOver();

        return new MoveResult(true, null, isGameOver, resultWinner, false, promotion, from, to,
                movingType, capture);
    }

    private MoveResult fail(String error) {
        return new MoveResult(false, error, false, null, false, false, null, null, '\0', false);
    }

    private static Board createInitialBoard() {
        String[][] initialPosition = {
                {"BR", "BN", "BB", "BQ", "BK", "BB", "BN", "BR"},
                {"BP", "BP", "BP", "BP", "BP", "BP", "BP", "BP"},
                {".", ".", ".", ".", ".", ".", ".", "."},
                {".", ".", ".", ".", ".", ".", ".", "."},
                {".", ".", ".", ".", ".", ".", ".", "."},
                {".", ".", ".", ".", ".", ".", ".", "."},
                {"WP", "WP", "WP", "WP", "WP", "WP", "WP", "WP"},
                {"WR", "WN", "WB", "WQ", "WK", "WB", "WN", "WR"}
        };
        return new Board(initialPosition);
    }

    private static char colorChar(String color) {
        if ("WHITE".equalsIgnoreCase(color)) return 'W';
        if ("BLACK".equalsIgnoreCase(color)) return 'B';
        return 0;
    }

    private static String colorString(char color) {
        return color == 'W' ? "WHITE" : "BLACK";
    }

    private static String mapWinner(String engineWinner) {
        if ("White".equals(engineWinner)) return "WHITE";
        if ("Black".equals(engineWinner)) return "BLACK";
        if ("Draw".equals(engineWinner)) return "DRAW";
        return engineWinner;
    }

    private static String formatMove(char color, Position from, Position to, Piece piece, boolean capture) {
        return "" + color + piece.getType() + " " + from.getRow() + "," + from.getCol() +
                " -> " + to.getRow() + "," + to.getCol() + (capture ? " x" : "");
    }

    public static class MoveResult {
        public final boolean success;
        public final String error;
        public final boolean gameOver;
        public final String winner;
        public final boolean check;
        public final boolean promotion;
        public final Position from;
        public final Position to;
        public final char pieceType;
        public final boolean capture;

        public MoveResult(boolean success, String error, boolean gameOver, String winner,
                          boolean check, boolean promotion, Position from, Position to,
                          char pieceType, boolean capture) {
            this.success = success;
            this.error = error;
            this.gameOver = gameOver;
            this.winner = winner;
            this.check = check;
            this.promotion = promotion;
            this.from = from;
            this.to = to;
            this.pieceType = pieceType;
            this.capture = capture;
        }
    }
}
