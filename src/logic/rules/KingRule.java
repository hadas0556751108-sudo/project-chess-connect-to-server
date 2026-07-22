package logic.rules;
import model.Piece;
import model.Position;

public class KingRule implements MoveRule {
    @Override
    public boolean isLegal(Piece[][] grid, Position from, Position to) {
        int rowDiff = Math.abs(from.getRow() - to.getRow());
        int colDiff = Math.abs(from.getCol() - to.getCol());

        // מלך זז לכל היותר משבצת אחת בכל כיוון
        return rowDiff <= 1 && colDiff <= 1 && (rowDiff + colDiff > 0);
    }
}