package logic.rules;
import model.Piece;
import model.Position;

public class KnightRule implements MoveRule {
    @Override
    public boolean isLegal(Piece[][] grid, Position from, Position to) {
        int rowDiff = Math.abs(from.getRow() - to.getRow());
        int colDiff = Math.abs(from.getCol() - to.getCol());

        
        return (rowDiff == 2 && colDiff == 1) || (rowDiff == 1 && colDiff == 2);
    }
}