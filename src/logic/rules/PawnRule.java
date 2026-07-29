package logic.rules;
import model.Piece;
import model.Position;

public class PawnRule implements MoveRule {
    @Override
    public boolean isLegal(Piece[][] grid, Position from, Position to) {
        int rowDiff = to.getRow() - from.getRow();
        int colDiff = to.getCol() - from.getCol();
        Piece p = grid[from.getRow()][from.getCol()];

        if (p == null) return false;

        int forward = (p.getColor() == 'W') ? -1 : 1;
        boolean isFirstMove = !p.hasMoved();

       
        boolean isStartingPosition = (p.getColor() == 'W' && from.getRow() == 6) ||
                                     (p.getColor() == 'B' && from.getRow() == 1);

        
        if (rowDiff == forward && colDiff == 0 && grid[to.getRow()][to.getCol()] == null) {
            return true;
        }

        
        if (isFirstMove && isStartingPosition && rowDiff == 2 * forward && colDiff == 0) {
            int midRow = from.getRow() + forward;
            if (grid[midRow][from.getCol()] == null && grid[to.getRow()][to.getCol()] == null) {
                return true;
            }
        }

        
        if (rowDiff == forward && Math.abs(colDiff) == 1) {
            Piece target = grid[to.getRow()][to.getCol()];
            if (target != null && target.getColor() != p.getColor()) {
                return true;
            }
        }

        return false;
    }
}
