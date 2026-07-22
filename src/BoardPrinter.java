
import model.Piece;

public class BoardPrinter {

    public static void printBoard(Piece[][] grid) {
        for (int i = 0; i < grid.length; i++) {
            for (int j = 0; j < grid[i].length; j++) {

                if (grid[i][j] == null) {
                    System.out.print(".");
                } else {
                    System.out.print(grid[i][j].toString());
                }
                if (j < grid[i].length - 1) System.out.print(" ");
            }
            System.out.println();
        }
    }
}