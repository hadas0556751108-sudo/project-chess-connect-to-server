package logic;

import logic.rules.MoveRule;
import model.Board;
import model.Piece;
import model.Position;

public class MoveValidator {

    public static boolean isLegalMove(Piece[][] grid, Position from, Position to, MoveRule rule) {
        // 1. בדיקת קלט בסיסית
        if (grid == null || grid.length == 0 || grid[0] == null || from == null || to == null) return false;
        if (from.equals(to)) return false;

        int rows = grid.length;
        int cols = grid[0].length;

        // 2. גבולות הלוח (מקור ויעד)
        if (from.getRow() < 0 || from.getRow() >= rows || from.getCol() < 0 || from.getCol() >= cols) return false;
        if (to.getRow() < 0 || to.getRow() >= rows || to.getCol() < 0 || to.getCol() >= cols) return false;

        Piece attacker = grid[from.getRow()][from.getCol()];
        Piece target = grid[to.getRow()][to.getCol()];

        // 3. ודאות שיש כלי במקור
        if (attacker == null) return false;

        // 4. בדיקת אכילת חבר (חובה לבדוק ש-target לא null לפני שפונים אליו!)
        if (target != null) {
            if (target.getColor() == attacker.getColor()) {
                return false; // חסימה!
            }
        }

        // 5. עכשיו רק אם הכל בסדר, בודקים את החוקים של הכלי
        return rule.isLegal(grid, from, to);
    }
}
