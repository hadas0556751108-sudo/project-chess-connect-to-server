package logic;
import model.Piece;

public class PathValidator {
    public static boolean isPathClear(Piece[][] grid, int r1, int c1, int r2, int c2) {
        int rowStep = Integer.signum(r2 - r1);
        int colStep = Integer.signum(c2 - c1);

        int r = r1 + rowStep;
        int c = c1 + colStep;

        while (r != r2 || c != c2) {
            if (grid[r][c] != null) return false; // חסום!
            r += rowStep;
            c += colStep;
        }
        return true;
    }
}