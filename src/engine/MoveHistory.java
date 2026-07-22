package engine;

import model.Position;
import java.util.ArrayList;
import java.util.List;

public class MoveHistory {
    public static class MoveRecord {
        private final char pieceType;
        private final char pieceColor;
        private final Position from;
        private final Position to;
        private final long timestamp;

        public MoveRecord(char pieceType, char pieceColor, Position from, Position to, long timestamp) {
            this.pieceType = pieceType;
            this.pieceColor = pieceColor;
            this.from = from;
            this.to = to;
            this.timestamp = timestamp;
        }

        public char pieceType() { return pieceType; }
        public char pieceColor() { return pieceColor; }
        public Position from() { return from; }
        public Position to() { return to; }
        public long timestamp() { return timestamp; }

        @Override
        public String toString() {
            return "" + pieceColor + pieceType + " " + from + " -> " + to;
        }
    }

    private final List<MoveRecord> whiteMoves = new ArrayList<>();
    private final List<MoveRecord> blackMoves = new ArrayList<>();
    private final List<MoveHistoryObserver> observers = new ArrayList<>();

    public interface MoveHistoryObserver {
        void onMoveAdded(MoveRecord move, boolean isWhite);
    }

    public void addObserver(MoveHistoryObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(MoveHistoryObserver observer) {
        observers.remove(observer);
    }

    public void addMove(char pieceType, char pieceColor, Position from, Position to, long timestamp) {
        MoveRecord move = new MoveRecord(pieceType, pieceColor, from, to, timestamp);
        boolean isWhite = (pieceColor == 'W');
        
        if (isWhite) {
            whiteMoves.add(move);
        } else {
            blackMoves.add(move);
        }

        // הודעה ל-observers ב-thread נפרד כדי לא להאט את המשחק
        notifyObservers(move, isWhite);
    }

    private void notifyObservers(MoveRecord move, boolean isWhite) {
        for (MoveHistoryObserver observer : observers) {
            try {
                observer.onMoveAdded(move, isWhite);
            } catch (Exception e) {
                // לא נרצה ש-observer ישבור את המשחק
                e.printStackTrace();
            }
        }
    }

    public List<MoveRecord> getWhiteMoves() {
        return new ArrayList<>(whiteMoves);
    }

    public List<MoveRecord> getBlackMoves() {
        return new ArrayList<>(blackMoves);
    }

    public void reset() {
        whiteMoves.clear();
        blackMoves.clear();
    }
}
