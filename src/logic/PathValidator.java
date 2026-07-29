package logic;
import model.Piece;

public class PathValidator {
    public static boolean isPathClear(Piece[][] grid, int r1, int c1, int r2, int c2) {
        
        if (r1 == r2 && c1 == c2) return false;

        
        if (grid == null || grid.length == 0 || grid[0] == null) return false;
        if (r1 < 0 || r1 >= grid.length || r2 < 0 || r2 >= grid.length ||
            c1 < 0 || c1 >= grid[0].length || c2 < 0 || c2 >= grid[0].length) return false;

        int rowStep = Integer.signum(r2 - r1);
        int colStep = Integer.signum(c2 - c1);

        int r = r1 + rowStep;
        int c = c1 + colStep;

        while (r != r2 || c != c2) {
            if (grid[r][c] != null) return false; 
            r += rowStep;
            c += colStep;
        }
        return true;
    }
}
