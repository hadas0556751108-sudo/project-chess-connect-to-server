package engine;

import input.BoardMapper;
import model.Piece;
import model.Position;
import network.ChessClient;
import network.MessageHandler;
import network.Protocol;
import ui.SoundPlayer;

import javax.swing.SwingUtilities;
import java.util.function.Consumer;

public class GameController implements GameEventListener, MessageHandler {
    private final GameEngine gameEngine;
    private final BoardMapper boardMapper;
    private final ChessClient client;
    private final String color;
    private final boolean networkMode;
    private final Consumer<String> gameOverCallback;
    private volatile boolean gameOverHandled;
    private Position lastClickPos;
    private long lastClickTime;
    private static final long DOUBLE_CLICK_INTERVAL_MS = 300;

    public GameController(GameEngine engine, BoardMapper boardMapper) {
        this(engine, boardMapper, null, null, null);
    }

    public GameController(GameEngine engine, BoardMapper boardMapper, ChessClient client, String color, Consumer<String> gameOverCallback) {
        this.gameEngine = engine;
        this.boardMapper = boardMapper;
        this.client = client;
        this.color = color;
        this.networkMode = (client != null);
        this.gameOverCallback = gameOverCallback;

        this.gameEngine.addListener(this);
        if (this.client != null) {
            this.client.addMessageHandler(this);
        }
    }

    public void click(int pixelX, int pixelY) {
        Position pos = boardMapper.pixelsToPosition(pixelX, pixelY);
        if (pos == null) return;

        long now = System.currentTimeMillis();
        if (lastClickPos != null && lastClickPos.equals(pos)
                && (now - lastClickTime) <= DOUBLE_CLICK_INTERVAL_MS) {
            lastClickPos = null;
            lastClickTime = 0;
            performJump(pos);
            return;
        }

        Piece clicked = gameEngine.getBoard().getPieceAt(pos);
        if (clicked != null && clicked.getColor() == getPlayerColorChar()) {
            SoundPlayer.play("click");
        }
        gameEngine.processClick(pos);
        lastClickPos = pos;
        lastClickTime = now;
    }

    public void doubleClick(int pixelX, int pixelY) {
        Position pos = boardMapper.pixelsToPosition(pixelX, pixelY);
        if (pos != null) {
            performJump(pos);
        }
    }

    private void performJump(Position pos) {
        Piece piece = gameEngine.getBoard().getPieceAt(pos);
        if (piece == null) return;

        
        if (networkMode) {
            char playerColor = getPlayerColorChar();
            if (playerColor == 0 || piece.getColor() != playerColor) {
                return;
            }
        }

        gameEngine.processJump(pos);
    }

    public void handleInput(String command) {
        if (command.startsWith("click")) {
            String[] parts = command.split(" ");
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);

            Position pos = boardMapper.pixelsToPosition(x, y);
            gameEngine.processClick(pos);
        } else if (command.startsWith("jump")) {
            String[] parts = command.split(" ");
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            Position pos = boardMapper.pixelsToPosition(x, y);
            gameEngine.processJump(pos);
        }
    }

    @Override
    public void onMoveRequested(Position from, Position to) {
        if (networkMode) {
            Piece moving = gameEngine.getBoard().getPieceAt(from);
            char playerColor = getPlayerColorChar();
            System.out.println("[GameController] onMoveRequested: from=" + from + " to=" + to + " color=" + color + " playerColor=" + playerColor);
            if (moving == null || playerColor == 0 || moving.getColor() != playerColor) {
                System.out.println("[GameController] onMoveRequested blocked: moving=" + moving + " playerColor=" + playerColor);
                return;
            }
            boolean capture = gameEngine.getBoard().getPieceAt(to) != null;
            sendMove(from, to, capture);
        } else {
            gameEngine.executeMove(from, to);
        }
    }

    @Override
    public void onMoveExecuted(Position from, Position to) {
        // The board animates via RealTimeManager; no extra routing needed here.
    }

    @Override
    public void onGameOver(String winner) {
        if (gameOverHandled) {
            return;
        }
        gameOverHandled = true;

        if (gameOverCallback != null) {
            SwingUtilities.invokeLater(() -> gameOverCallback.accept(winner));
        }

        if (networkMode && client != null && winner != null &&
                ("DRAW".equals(winner) || winner.equals(color))) {
            try {
                client.sendCommand(Protocol.GAME_OVER + " " + winner);
            } catch (Exception e) {
                System.err.println("[GameController] failed to send GAME_OVER: " + e.getMessage());
            }
        }
    }

    @Override
    public void handleMessage(String message) {
        System.out.println("[GameController] handleMessage: " + message);
        SwingUtilities.invokeLater(() -> handleNetworkMessage(message));
    }

    private void handleNetworkMessage(String message) {
        System.out.println("[GameController] handleNetworkMessage: " + message);
        if (message == null) {
            System.out.println("[GameController] handleNetworkMessage null");
            return;
        }

        String[] parts = message.trim().split("\\s+");
        String type = parts[0];
        System.out.println("[GameController] message type: " + type + " parts.length: " + parts.length);

        if (Protocol.MOVE.equals(type) && parts.length >= 6) {
            try {
                int fromRow = Integer.parseInt(parts[2]);
                int fromCol = Integer.parseInt(parts[3]);
                int toRow = Integer.parseInt(parts[4]);
                int toCol = Integer.parseInt(parts[5]);
                System.out.println("[GameController] MOVE parsed: (" + fromRow + "," + fromCol + ") -> (" + toRow + "," + toCol + ")");
                gameEngine.executeMove(new Position(fromRow, fromCol), new Position(toRow, toCol));
            } catch (NumberFormatException e) {
                System.out.println("[GameController] MOVE parse failed: " + message);
            }
        }
    }

    public GameEngine getGameEngine() {
        return this.gameEngine;
    }

    public Position getSelectedPosition() {
        return gameEngine.getSelectedPosition();
    }

    private void sendMove(Position from, Position to, boolean capture) {
        if (client == null) return;
        String command = Protocol.MOVE + " "
                + from.getRow() + " " + from.getCol() + " "
                + to.getRow() + " " + to.getCol() + " "
                + capture;
        System.out.println("[GameController] sendMove: " + command);
        client.sendCommand(command);
    }

    private char getPlayerColorChar() {
        if ("WHITE".equals(color)) return 'W';
        if ("BLACK".equals(color)) return 'B';
        return 0;
    }

}
