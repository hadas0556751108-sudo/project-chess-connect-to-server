package logic.rules;
import model.Piece;
import model.Position;
import logic.PathValidator;

public class BishopRule implements MoveRule {
    @Override
    public boolean isLegal(Piece[][] grid, Position from, Position to) {
        int rowDiff = Math.abs(from.getRow() - to.getRow());
        int colDiff = Math.abs(from.getCol() - to.getCol());

        // רץ זז רק באלכסון (ההפרש בשורות שווה להפרש בעמודות)
        if (rowDiff != colDiff) return false;

        // בדיקה שהדרך פנויה
        return PathValidator.isPathClear(grid, from.getRow(), from.getCol(), to.getRow(), to.getCol());
    }
}