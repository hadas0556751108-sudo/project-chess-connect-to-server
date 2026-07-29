package input;

import model.Position;

public class BoardMapper {
    private final int cellSize;
    private final int boardWidth;
    private final int boardHeight;

    public BoardMapper(int cellSize, int boardWidth, int boardHeight) {
        this.cellSize = cellSize;
        this.boardWidth = boardWidth;
        this.boardHeight = boardHeight;
    }

    public Position pixelsToPosition(int pixelX, int pixelY) {
        int col = pixelX / cellSize;
        int row = pixelY / cellSize;

        if (col < 0 || col >= boardWidth || row < 0 || row >= boardHeight) {
            return null; 
        }

        return new Position(row, col);
    }

    public int positionToPixelX(Position pos) {
        return pos.getCol() * cellSize;
    }

    public int positionToPixelY(Position pos) {
        return pos.getRow() * cellSize;
    }

    public int getCellSize() {
        return cellSize;
    }
}
