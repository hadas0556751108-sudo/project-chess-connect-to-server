package engine;
import model.Board;
import model.MoveEvent;
import model.Piece;
import model.Position;

import java.util.ArrayList;
import java.util.List;

public class RealTimeManager {
    private List<MoveEvent>eventQueue=new ArrayList<>();
    public void addEvent(MoveEvent event){
        eventQueue.add(event);
    }

    private long gameClock = 0;

    public boolean hasPendingMoves() {
        for (MoveEvent e : eventQueue) {
            if (gameClock < e.getArrivalTime()) {
                return true;
            }
        }
        return false;
    }

    public void advanceClock(long ms, Board board, GameEngine engine) {
        gameClock += ms;
        eventQueue.sort((a, b) -> Long.compare(a.getArrivalTime(), b.getArrivalTime()));

        // עדכן states של כלים שלא בתנועה (rest states)
        updateRestStates(board, engine);

        long currentTime = gameClock;

        for (int i = 0; i < eventQueue.size(); i++) {
            MoveEvent event = eventQueue.get(i);
            if (currentTime >= event.getArrivalTime() && !event.isJump()) {
                boolean capturedByAirborne = false;
                for (int j = 0; j < eventQueue.size(); j++) {
                    MoveEvent other = eventQueue.get(j);
                    if (other.isJump() && currentTime <= other.getArrivalTime()
                            && other.getFrom().getRow() == event.getTo().getRow()
                            && other.getFrom().getCol() == event.getTo().getCol()) {
                        if (other.getPiece() != null && event.getPiece() != null
                                && other.getPiece().getColor() != event.getPiece().getColor()) {
                            board.updatePiece(event.getFrom(), null);
                            capturedByAirborne = true;
                            break;
                        }
                    }
                }

                if (!capturedByAirborne) {
                    // Capture logic: אם יש כלי אויב במיקום היעד, הוא יוסר
                    Piece target = board.getPieceAt(event.getTo());
                    if (target != null && target.getColor() != event.getPiece().getColor()) {
                        engine.getScoreManager().addCapture(target.getColor(), target.getType());
                    }
                    // הכלי המגיע מוצב במיקום היעד (זה מחליף את מה שיש שם)
                    board.updatePiece(event.getFrom(), null);
                    board.updatePiece(event.getTo(), event.getPiece());
                    if (event.getPiece() != null) event.getPiece().setHasMoved(true);
                    engine.postMoveActions(event.getTo());

                    // הוספה להיסטוריית מהלכים
                    engine.getMoveHistory().addMove(
                        event.getPiece().getType(),
                        event.getPiece().getColor(),
                        event.getFrom(),
                        event.getTo(),
                        event.getArrivalTime()
                    );

                    // State machine: עבור ל-state הבא אחרי המהלך
                    transitionPieceState(event.getPiece(), "move", engine);
                }

                eventQueue.remove(i);
                i--;
                if (!engine.isGameOver()) {
                    engine.nextTurn();
                }
                engine.checkGameOver(board);
            }
        }

        for (int i = 0; i < eventQueue.size(); i++) {
            MoveEvent event = eventQueue.get(i);
            if (currentTime >= event.getArrivalTime() && event.isJump()) {
                // Jump מעדכן את הלוח כמו מהלך רגיל
                Piece target = board.getPieceAt(event.getTo());
                if (target != null && target.getColor() != event.getPiece().getColor()) {
                    engine.getScoreManager().addCapture(target.getColor(), target.getType());
                }
                board.updatePiece(event.getFrom(), null);
                board.updatePiece(event.getTo(), event.getPiece());
                // קפיצה במקום היא פעולה בזמן אמת ולא צורכת תור - לא מסמנים hasMoved
                // כדי לא לפגוע בהרשאת המהלך הראשון של חיילים

                // הוספה להיסטוריית מהלכים
                engine.getMoveHistory().addMove(
                    event.getPiece().getType(),
                    event.getPiece().getColor(),
                    event.getFrom(),
                    event.getTo(),
                    event.getArrivalTime()
                );

                // State machine: עבור ל-state הבא אחרי הקפיצה
                transitionPieceState(event.getPiece(), "jump", engine);

                eventQueue.remove(i);
                i--;
                engine.checkGameOver(board);
            }
        }
    }

    public long getCurrentTime() {
        return gameClock;
    }

    public List<MoveEvent> getActiveEvents() {
        List<MoveEvent> active = new ArrayList<>();
        for (MoveEvent e : eventQueue) {
            if (gameClock < e.getArrivalTime()) {
                active.add(e);
            }
        }
        return active;
    }

    private void transitionPieceState(Piece piece, String currentStateName, GameEngine engine) {
        if (piece == null) return;

        String pieceKey = Character.toLowerCase(piece.getColor()) + "" + piece.getType();
        StateConfig config = engine.getConfigLoader().getConfig(pieceKey, currentStateName);

        if (config == null) {
            // אם אין config, חזור ל-IDLE
            piece.setCurrentState(Piece.State.IDLE);
            piece.setStateStartTime(gameClock);
            return;
        }

        String nextStateName = config.nextStateWhenFinished();
        if (nextStateName == null) {
            piece.setCurrentState(Piece.State.IDLE);
            piece.setStateStartTime(gameClock);
            return;
        }

        // המר את שם ה-state ל-enum
        switch (nextStateName.toLowerCase()) {
            case "idle":
                piece.setCurrentState(Piece.State.IDLE);
                break;
            case "short_rest":
                piece.setCurrentState(Piece.State.SHORT_REST);
                break;
            case "long_rest":
                piece.setCurrentState(Piece.State.LONG_REST);
                break;
            default:
                piece.setCurrentState(Piece.State.IDLE);
        }
        piece.setStateStartTime(gameClock);
    }

    private void updateRestStates(Board board, GameEngine engine) {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece piece = board.getPieceAt(new Position(r, c));
                if (piece == null) continue;

                // בדוק אם הכלי ב-rest state
                if (piece.getCurrentState() == Piece.State.SHORT_REST ||
                    piece.getCurrentState() == Piece.State.LONG_REST) {

                    String stateName = piece.getCurrentState() == Piece.State.SHORT_REST ?
                                      "short_rest" : "long_rest";
                    String pieceKey = Character.toLowerCase(piece.getColor()) + "" + piece.getType();
                    StateConfig config = engine.getConfigLoader().getConfig(pieceKey, stateName);

                    if (config != null && !config.isLoop()) {
                        // אם ה-state לא מתחלף והזמן עבר, עבור ל-state הבא
                        long elapsed = piece.getStateElapsedMillis(gameClock);
                        // חשב את הזמן המקסימלי של ה-state מה-config
                        int framesPerSec = config.framesPerSec();
                        int frameCount = config.frameCount();
                        long maxDuration = (long) ((frameCount * 1000.0) / framesPerSec);

                        if (elapsed >= maxDuration) {
                            // לאחר כל rest, חזור ל-IDLE
                            piece.setCurrentState(Piece.State.IDLE);
                            piece.setStateStartTime(gameClock);
                        }
                    }
                }
            }
        }
    }
}
