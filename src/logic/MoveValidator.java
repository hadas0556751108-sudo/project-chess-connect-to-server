package logic;

import logic.rules.MoveRule;
import model.Board;
import model.Piece;
import model.Position;

public class MoveValidator {

    public static boolean isLegalMove(Piece[][] grid, Position from, Position to, MoveRule rule) {
        // 1. גבולות הלוח
        if (to.getRow() < 0 || to.getRow() >= 8 || to.getCol() < 0 || to.getCol() >= 8) return false;

        Piece attacker = grid[from.getRow()][from.getCol()];
        Piece target = grid[to.getRow()][to.getCol()];

        // 2. בדיקת אכילת חבר (חובה לבדוק ש-target לא null לפני שפונים אליו!)
        if (target != null) {
            if (target.getColor() == attacker.getColor()) {
                return false; // חסימה!
            }
        }

        // 3. עכשיו רק אם הכל בסדר, בודקים את החוקים של הכלי
        return rule.isLegal(grid, from, to);
    }
}