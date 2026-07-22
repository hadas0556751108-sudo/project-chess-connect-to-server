package engine;

import engine.RealTimeManager;
import engine.StateConfigLoader;
import model.Board;
import model.MoveEvent;
import model.Position;
import model.Piece;
import logic.RulesEngine;
import java.util.ArrayList;
import java.util.List;

public class GameEngine {
    private Board board;
    private RealTimeManager timeManager;
    private RulesEngine rules;
    private StateConfigLoader configLoader;
    private Position selected = null;
    private boolean isGameOver = false;
    private String winner;
    private char currentTurn = 'W';
    private ScoreManager scoreManager;
    private MoveHistory moveHistory;
    private List<GameEventListener> listeners = new ArrayList<>();

    public GameEngine(Board board, RealTimeManager timeManager, StateConfigLoader configLoader) {
        this.board = board;
        this.timeManager = timeManager;
        this.rules = new RulesEngine();
        this.configLoader = configLoader;
        this.scoreManager = new ScoreManager();
        this.moveHistory = new MoveHistory();
    }

    public void addListener(GameEventListener listener) {
        listeners.add(listener);
    }

    // בתוך GameEngine.java
    public void processClick(Position pos) {
        // מניעת לחיצות אם יש מהלך בתהליך!
        if (this.isGameOver) return;
        
        System.out.println("Click at: " + pos);
        
        // בודק אם הכלי במיקום הזה כבר נמצא בתנועה
        if (isPieceMoving(pos)) {
            System.out.println("Piece is moving, click ignored");
            return;
        }

        Piece clickedPiece = board.getPieceAt(pos);
       
        System.out.println("selected: " + selected + ", clickedPiece: " + clickedPiece);
        
        if (selected == null) {
            if (clickedPiece != null) {
                selected = pos;
                System.out.println("Selected piece at: " + selected);
            }
        } else {
            // אם לחצנו על כלי שלנו - מחליפים בחירה
            Piece selectedPiece = board.getPieceAt(selected);
            System.out.println("selectedPiece: " + selectedPiece + ", clickedPiece: " + clickedPiece);
            if (clickedPiece != null && selectedPiece != null) {
                System.out.println("clickedPiece color: " + clickedPiece.getColor() + ", selectedPiece color: " + selectedPiece.getColor());
                if (clickedPiece.getColor() == selectedPiece.getColor()) {
                    selected = pos;
                    System.out.println("Changed selection to: " + selected);
                    return;
                }
            }
            // ניסיון מהלך
            System.out.println("Attempting move from " + selected + " to " + pos);
            if (rules.isMoveLegal(board.getGrid(), selected, pos)) {
                notifyMoveRequested(selected, pos);
            }
            selected = null;
        }
    }

    public void processJump(Position pos) {
        if (this.isGameOver) return;
        if (timeManager.hasPendingMoves()) return;

        Piece p = board.getPieceAt(pos);
        if (p == null) return;

        // חשב duration מה-config
        String pieceKey = Character.toLowerCase(p.getColor()) + "" + p.getType();
        StateConfig config = configLoader.getConfig(pieceKey, "jump");
        double speedMetersPerSec = (config != null) ? config.speedMetersPerSec() : 0.3;
        
        // קפיצה היא במקום, אז המרחק הוא 0, אבל נשתמש במרחק קטן כדי שה-duration לא יהיה 0
        double distanceMeters = 0.5; // חצי מטר לקפיצה במקום
        double durationSec = distanceMeters / speedMetersPerSec;
        long duration = (long) (durationSec * 1000);
        
        // שנה את state של הכלי ל-JUMPING
        p.setCurrentState(Piece.State.JUMPING);
        p.setStateStartTime(timeManager.getCurrentTime());
        
        // השתמש ב-gameClock כדי שה-MoveEvent יתבצע בזמן הנכון
        long arrivalTime = timeManager.getCurrentTime() + duration;
        MoveEvent jump = new MoveEvent(p, pos, pos, arrivalTime, true);
        timeManager.addEvent(jump);
    }

    public void executeMove(Position from, Position to) {
        if (this.isGameOver) return;
        if (isPieceMoving(from)) return;

        Piece moving = board.getPieceAt(from);
        if (moving == null) return;

        if (!rules.isMoveLegal(board.getGrid(), from, to)) return;

        String pieceKey = Character.toLowerCase(moving.getColor()) + "" + moving.getType();
        StateConfig config = configLoader.getConfig(pieceKey, "move");
        double speedMetersPerSec = (config != null) ? config.speedMetersPerSec() : 0.3;

        int rowDiff = Math.abs(to.getRow() - from.getRow());
        int colDiff = Math.abs(to.getCol() - from.getCol());
        double distanceCells = Math.sqrt(rowDiff * rowDiff + colDiff * colDiff);
        double distanceMeters = distanceCells * 1.0;

        double durationSec = distanceMeters / speedMetersPerSec;
        long duration = (long) (durationSec * 1000);

        moving.setCurrentState(Piece.State.MOVING);
        moving.setStateStartTime(timeManager.getCurrentTime());

        long arrivalTime = timeManager.getCurrentTime() + duration;
        timeManager.addEvent(new MoveEvent(moving, from, to, arrivalTime));

        notifyMoveExecuted(from, to);
    }

    public void postMoveActions(Position to) {
        Piece p = board.getPieceAt(to);
        if (p == null) return;

        // הכתרה: חייל המגיע לשורה האחרונה של הלוח
        if (p.getType() == 'P') {
            Piece[][] grid = board.getGrid();
            int lastRow = grid.length - 1;
            // חייל לבן נע לכיוון מעלה (שורה קטנה יותר), חייל שחור לכיוון מטה (שורה גדולה יותר)
            boolean promoteWhite = (p.getColor() == 'W' && to.getRow() == 0);
            boolean promoteBlack = (p.getColor() == 'B' && to.getRow() == lastRow);
            if (promoteWhite || promoteBlack) {
                Piece queen = new Piece(p.getColor(), 'Q');
                queen.setHasMoved(true); // המלכה יורשת את ה-hasMoved מהחייל
                board.updatePiece(to, queen);
            }
        }
    }
    public void checkGameOver(Board board) {
        if (this.isGameOver) {
            return;
        }

        boolean whiteKingExists = false;
        boolean blackKingExists = false;

        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                Piece p = board.getPieceAt(new Position(i, j));
                if (p != null && p.getType() == 'K') {
                    if (p.getColor() == 'W') whiteKingExists = true;
                    if (p.getColor() == 'B') blackKingExists = true;
                }
            }
        }

        if (!whiteKingExists || !blackKingExists) {
            this.isGameOver = true;
            if (!whiteKingExists && !blackKingExists) {
                this.winner = "Draw";
            } else if (!whiteKingExists) {
                this.winner = "Black";
            } else {
                this.winner = "White";
            }
            notifyGameOver(this.winner.equals("White") ? "WHITE" : (this.winner.equals("Black") ? "BLACK" : "DRAW"));
            return;
        }

        if (!hasLegalMoves(this.currentTurn)) {
            this.isGameOver = true;
            this.winner = "Draw";
            notifyGameOver("DRAW");
        }
    }

    private boolean hasLegalMoves(char color) {
        Piece[][] grid = board.getGrid();
        for (int r = 0; r < grid.length; r++) {
            for (int c = 0; c < grid[r].length; c++) {
                Piece p = grid[r][c];
                if (p != null && p.getColor() == color) {
                    Position from = new Position(r, c);
                    for (int tr = 0; tr < grid.length; tr++) {
                        for (int tc = 0; tc < grid[tr].length; tc++) {
                            Position to = new Position(tr, tc);
                            if (rules.isMoveLegal(grid, from, to)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    // בתוך GameEngine.java
    public boolean isGameOver() {
        return this.isGameOver;
    }

    public String getWinner() {
        return this.winner;
    }

    public char getCurrentTurn() {
        return this.currentTurn;
    }

    public void nextTurn() {
        this.currentTurn = (this.currentTurn == 'W') ? 'B' : 'W';
    }

    public GameSnapshot snapshot(Position selectedPosition) {
        List<PieceSnapshot> pieceSnapshots = new ArrayList<>();
        int cellSize = 100; // גודל תא בפיקסלים

        // עובר על כל הכלים בלוח
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Position pos = new Position(r, c);
                Piece piece = board.getPieceAt(pos);
                if (piece != null) {
                    // בודק אם הכלי בתנועה
                    MoveEvent activeEvent = findActiveEventForPiece(piece);
                    double pixelX, pixelY;
                    PieceVisualState state;
                    long stateElapsed = 0;

                    if (activeEvent != null) {
                        // כלי בתנועה - חשב מיקום מבוסס progress
                        long currentTime = timeManager.getCurrentTime();
                        long arrivalTime = activeEvent.getArrivalTime();
                        
                        // חשב duration מה-config
                        String pieceKey = Character.toLowerCase(piece.getColor()) + "" + piece.getType();
                        String stateName = activeEvent.isJump() ? "jump" : "move";
                        StateConfig config = configLoader.getConfig(pieceKey, stateName);
                        double speedMetersPerSec = (config != null) ? config.speedMetersPerSec() : 0.3;
                        
                        // חשב מרחק במטרים
                        Position from = activeEvent.getFrom();
                        Position to = activeEvent.getTo();
                        double distanceCells = Math.sqrt(
                                Math.pow(to.getCol() - from.getCol(), 2) +
                                Math.pow(to.getRow() - from.getRow(), 2)
                        );
                        double distanceMeters = distanceCells * 1.0; // נניח שתא אחד = 1 מטר
                        
                        // חשב duration במילישניות
                        double durationSec = distanceMeters / speedMetersPerSec;
                        long duration = (long) (durationSec * 1000);
                        
                        long startTime = arrivalTime - duration;
                        long elapsed = currentTime - startTime;
                        double progress = Math.min(1.0, (double) elapsed / duration);
                        
                        // אם ה-progress הוא 1.0, הכלי הגיע ליעד - השתמש במיקום הסופי
                        if (progress >= 1.0) {
                            pixelX = to.getCol() * cellSize;
                            pixelY = to.getRow() * cellSize;
                        } else {
                            double startX = from.getCol() * cellSize;
                            double startY = from.getRow() * cellSize;
                            double endX = to.getCol() * cellSize;
                            double endY = to.getRow() * cellSize;

                            pixelX = startX + (endX - startX) * progress;
                            pixelY = startY + (endY - startY) * progress;
                        }

                        state = activeEvent.isJump() ? PieceVisualState.JUMPING : PieceVisualState.MOVING;
                        stateElapsed = piece.getStateElapsedMillis(timeManager.getCurrentTime());
                    } else {
                        // כלי עומד במקום - השתמש ב-state מה-Piece
                        pixelX = c * cellSize;
                        pixelY = r * cellSize;
                        state = piece.getVisualState();
                        stateElapsed = piece.getStateElapsedMillis(timeManager.getCurrentTime());
                    }

                    String pieceId = "" + piece.getColor() + piece.getType() + r + c;
                    pieceSnapshots.add(new PieceSnapshot(
                            pieceId,
                            piece.getType(),
                            piece.getColor(),
                            state,
                            pixelX,
                            pixelY,
                            stateElapsed
                    ));
                }
            }
        }

        return new GameSnapshot(8, 8, pieceSnapshots, selectedPosition, isGameOver, this.winner);
    }

    private MoveEvent findActiveEventForPiece(Piece piece) {
        List<MoveEvent> activeEvents = timeManager.getActiveEvents();
        for (MoveEvent event : activeEvents) {
            if (event.getPiece() == piece) {
                return event;
            }
        }
        return null;
    }

    public RealTimeManager getRealTimeManager() {
        return timeManager;
    }

    public Board getBoard() {
        return board;
    }

    public Position getSelectedPosition() {
        return selected;
    }

    public ScoreManager getScoreManager() {
        return scoreManager;
    }

    public MoveHistory getMoveHistory() {
        return moveHistory;
    }

    public StateConfigLoader getConfigLoader() {
        return configLoader;
    }

    private void notifyMoveRequested(Position from, Position to) {
        for (GameEventListener listener : listeners) {
            listener.onMoveRequested(from, to);
        }
    }

    private void notifyMoveExecuted(Position from, Position to) {
        for (GameEventListener listener : listeners) {
            listener.onMoveExecuted(from, to);
        }
    }

    private void notifyGameOver(String winner) {
        for (GameEventListener listener : listeners) {
            listener.onGameOver(winner);
        }
    }

    // בודק אם הכלי במיקום הזה כבר נמצא בתנועה
    private boolean isPieceMoving(Position pos) {
        for (MoveEvent event : timeManager.getActiveEvents()) {
            if (event.getFrom().equals(pos) || event.getTo().equals(pos)) {
                return true;
            }
        }
        return false;
    }
}
