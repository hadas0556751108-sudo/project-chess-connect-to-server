
package logic.rules;
import model.Piece;
import model.Position;

public class PawnRule implements MoveRule { // סוגר מסולסל פתיחה ראשון
    @Override

    public boolean isLegal(Piece[][] grid, Position from, Position to) {
        int rowDiff = to.getRow() - from.getRow();
        int colDiff = to.getCol() - from.getCol();
        Piece p = grid[from.getRow()][from.getCol()];

        if (p == null) return false;

        int forward = (p.getColor() == 'W') ? -1 : 1;
        boolean isFirstMove = !p.hasMoved();
        
        // בדיקה אם החייל במיקום ההתחלתי (לבן בשורה 6, שחור בשורה 1)
        boolean isStartingPosition = (p.getColor() == 'W' && from.getRow() == 6) || 
                                     (p.getColor() == 'B' && from.getRow() == 1);

        // one square forward
        if (rowDiff == forward && colDiff == 0 && grid[to.getRow()][to.getCol()] == null) {
            return true;
        }

        // two squares forward on first move: path must be clear
        if (isFirstMove && isStartingPosition && rowDiff == 2 * forward && colDiff == 0) {
            int midRow = from.getRow() + forward;
            if (grid[midRow][from.getCol()] == null && grid[to.getRow()][to.getCol()] == null) {
                return true;
            }
        }

        // capture diagonally
        if (rowDiff == forward && Math.abs(colDiff) == 1) {
            Piece target = grid[to.getRow()][to.getCol()];
            if (target != null && target.getColor() != p.getColor()) {
                return true;
            }
        }

        return false;
    }

}