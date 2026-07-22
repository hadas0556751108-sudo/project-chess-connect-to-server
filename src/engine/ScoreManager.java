package engine;

public class ScoreManager {
    private int whiteScore = 0;
    private int blackScore = 0;

    public ScoreManager() {
    }

    public void addCapture(char capturedColor, char capturedType) {
        // ערכי הכלים: P=1, N=3, B=3, R=5, Q=9, K=0 (המלך לא נלכד, המשחק נגמר)
        int pieceValue = getPieceValue(capturedType);
        if (capturedColor == 'B') {
            whiteScore += pieceValue;
        } else {
            blackScore += pieceValue;
        }
    }

    private int getPieceValue(char type) {
        switch (type) {
            case 'P': return 1;
            case 'N': return 3;
            case 'B': return 3;
            case 'R': return 5;
            case 'Q': return 9;
            case 'K': return 0;
            default: return 0;
        }
    }

    public int getWhiteScore() {
        return whiteScore;
    }

    public int getBlackScore() {
        return blackScore;
    }

    public void reset() {
        whiteScore = 0;
        blackScore = 0;
    }
}
