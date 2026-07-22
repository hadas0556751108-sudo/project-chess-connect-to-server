package model;

public class Board {
    private Piece[][] grid;
    // תוסיפי את זה ל-Board.java
    public Board() {
        this.grid = new Piece[8][8];
    }
    public Board(String[][] rawData) {
        if (rawData == null || rawData.length == 0) {
            this.grid = new Piece[8][8]; // ברירת מחדל למקרה של קלט לא תקין
            return;
        }

        int rows = rawData.length;
        int cols = rawData[0].length;
        this.grid = new Piece[rows][cols];

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                String token = rawData[i][j];

                // נבדוק שהטוקן לא null, לא ".", ושיש לו לפחות 2 תווים
                if (token != null && !token.equals(".") && token.length() >= 2) {
                    char color = token.charAt(0);
                    char type = token.charAt(1);
                    this.grid[i][j] = new Piece(color, type);
                } else {
                    // אם זה ".", null, או משהו קצר מדי - נשים null בלוח
                    this.grid[i][j] = null;
                }
            }
        }
    }

    public Piece getPieceAt(Position pos) {
        // שינוי 2: הגנה על pos עצמו (למקרה שמישהו יעביר null ל-getPieceAt)
        if (pos == null || pos.getRow() < 0 || pos.getRow() >= grid.length ||
                pos.getCol() < 0 || pos.getCol() >= grid[0].length) {
            return null;
        }
        return grid[pos.getRow()][pos.getCol()];
    }

    public void updatePiece(Position pos, Piece piece) {

        // שינוי 3: כדאי להוסיף הגנה גם כאן
        if (pos != null && pos.getRow() >= 0 && pos.getRow() < grid.length &&
                pos.getCol() >= 0 && pos.getCol() < grid[0].length) {
            this.grid[pos.getRow()][pos.getCol()] = piece;
        }
    }

    public Piece[][] getGrid() { return grid; }
}