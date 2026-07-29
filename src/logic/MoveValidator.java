package logic;

import logic.rules.MoveRule;
import model.Board;
import model.Piece;
import model.Position;

public class MoveValidator {

    public static boolean isLegalMove(Piece[][] grid, Position from, Position to, MoveRule rule) {
        
        if (grid == null || grid.length == 0 || grid[0] == null || from == null || to == null) return false;
        if (from.equals(to)) return false;

        int rows = grid.length;
        int cols = grid[0].length;

       
        if (from.getRow() < 0 || from.getRow() >= rows || from.getCol() < 0 || from.getCol() >= cols) return false;
        if (to.getRow() < 0 || to.getRow() >= rows || to.getCol() < 0 || to.getCol() >= cols) return false;

        Piece attacker = grid[from.getRow()][from.getCol()];
        Piece target = grid[to.getRow()][to.getCol()];

        
        if (attacker == null) return false;

      
        if (target != null) {
            if (target.getColor() == attacker.getColor()) {
                return false; // חסימה!
            }
        }

       
        return rule.isLegal(grid, from, to);
    }
}
