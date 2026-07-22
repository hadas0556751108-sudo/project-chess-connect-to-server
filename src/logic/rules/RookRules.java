package logic.rules;
import model.Piece;
import model.Position;
import logic.PathValidator;

public class RookRules implements MoveRule {
    @Override
    public boolean isLegal(Piece[][] grid, Position from, Position to) {
        // צריח זז רק בקו ישר (row1 == row2 או col1 == col2)
        if (from.getRow() != to.getRow() && from.getCol() != to.getCol()) {
            return false;
        }
        // בדיקה שהדרך פנויה
        return PathValidator.isPathClear(grid, from.getRow(), from.getCol(), to.getRow(), to.getCol());
    }
}