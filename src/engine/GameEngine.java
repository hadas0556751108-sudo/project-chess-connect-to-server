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

  
    public void processClick(Position pos) {
      
        if (this.isGameOver) return;
        
        System.out.println("Click at: " + pos);
        
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

        
        String pieceKey = Character.toLowerCase(p.getColor()) + "" + p.getType();
        StateConfig config = configLoader.getConfig(pieceKey, "jump");
        double speedMetersPerSec = (config != null) ? config.speedMetersPerSec() : 0.3;
        
      
        long duration;
        if (speedMetersPerSec > 0) {
            double distanceMeters = 0.5; 
            double durationSec = distanceMeters / speedMetersPerSec;
            duration = (long) (durationSec * 1000);
        } else {
          
            if (config != null && config.frameCount() > 0 && config.framesPerSec() > 0) {
                duration = (long) (config.frameCount() * 1000.0 / config.framesPerSec());
            } else {
                duration = 500; 
            }
        }
        
    
        p.setCurrentState(Piece.State.JUMPING);
        p.setStateStartTime(timeManager.getCurrentTime());
        
        
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

        
        if (p.getType() == 'P') {
            Piece[][] grid = board.getGrid();
            int lastRow = grid.length - 1;
           
            boolean promoteWhite = (p.getColor() == 'W' && to.getRow() == 0);
            boolean promoteBlack = (p.getColor() == 'B' && to.getRow() == lastRow);
            if (promoteWhite || promoteBlack) {
                Piece queen = new Piece(p.getColor(), 'Q');
                queen.setHasMoved(true); 
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
        int cellSize = 100; 
      
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Position pos = new Position(r, c);
                Piece piece = board.getPieceAt(pos);
                if (piece != null) {
                  
                    MoveEvent activeEvent = findActiveEventForPiece(piece);
                    double pixelX, pixelY;
                    PieceVisualState state;
                    long stateElapsed = 0;

                    if (activeEvent != null) {
                     
                        long currentTime = timeManager.getCurrentTime();
                        long arrivalTime = activeEvent.getArrivalTime();
                        
                       
                        Position from = activeEvent.getFrom();
                        Position to = activeEvent.getTo();
                        long startTime = piece.getStateStartTime();
                        long duration = arrivalTime - startTime;
                        if (duration <= 0) {
                            duration = 1;
                        }
                        long elapsed = currentTime - startTime;
                        double progress = Math.min(1.0, (double) elapsed / duration);
                        
                        
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

   
    private boolean isPieceMoving(Position pos) {
        for (MoveEvent event : timeManager.getActiveEvents()) {
            if (event.getFrom().equals(pos) || event.getTo().equals(pos)) {
                return true;
            }
        }
        return false;
    }
}
