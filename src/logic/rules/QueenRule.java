package logic.rules;
import model.Piece;
import model.Position;
import logic.PathValidator; // הפונקציה שדיברנו עליה

public class QueenRule implements MoveRule {
    @Override
    public boolean isLegal(Piece[][] grid, Position from, Position to) {
        int rowDiff = Math.abs(from.getRow() - to.getRow());
        int colDiff = Math.abs(from.getCol() - to.getCol());

        // בדיקה גיאומטרית: האם זה אלכסון או קו ישר?
        boolean isPathValid = (rowDiff == colDiff || rowDiff == 0 || colDiff == 0);

        if (!isPathValid) return false;

        // בדיקה שהדרך פנויה (משתמשים ב-PathValidator)
        return PathValidator.isPathClear(grid, from.getRow(), from.getCol(), to.getRow(), to.getCol());
    }
}