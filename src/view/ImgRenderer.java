package view;

import engine.GameSnapshot;
import engine.PieceSnapshot;
import image.Img;

import java.awt.Color;

public class ImgRenderer {
    private final String boardImagePath;
    private final int cellSize;
    private final PieceImageLoader imageLoader;

    public ImgRenderer(String boardImagePath, int cellSize, PieceImageLoader imageLoader) {
        this.boardImagePath = boardImagePath;
        this.cellSize = cellSize;
        this.imageLoader = imageLoader;
    }

    public Img render(GameSnapshot snapshot) {
        int width = snapshot.boardWidth() * cellSize;
        int height = snapshot.boardHeight() * cellSize;

        Img canvas = new Img().read(
                boardImagePath,
                new java.awt.Dimension(width, height),
                false,
                null
        );

        drawPieces(canvas, snapshot);
        drawSelectedCell(canvas, snapshot);

        if (snapshot.gameOver()) {
            drawGameOver(canvas, snapshot);
        }

        return canvas;
    }

    private void drawPieces(Img canvas, GameSnapshot snapshot) {
        for (PieceSnapshot piece : snapshot.pieces()) {
            Img pieceImage = imageLoader.getFrame(piece);

            int x = (int) Math.round(piece.pixelX());
            int y = (int) Math.round(piece.pixelY());

            pieceImage.drawOn(canvas, x, y);
        }
    }

    private void drawSelectedCell(Img canvas, GameSnapshot snapshot) {
        model.Position selected = snapshot.selectedPosition();

        if (selected == null) {
            return;
        }

        int x = selected.getCol() * cellSize;
        int y = selected.getRow() * cellSize;

        canvas.drawRect(
                x,
                y,
                cellSize,
                cellSize,
                new Color(255, 215, 0, 180),
                4
        );
    }

    private void drawGameOver(Img canvas, GameSnapshot snapshot) {
        String winner = snapshot.winner();
        String text = winner == null ? "Game Over"
                : winner.equals("Draw") ? "Draw"
                : winner + " wins";

        canvas.putText(
                text,
                40,
                60,
                2.0f,
                Color.RED,
                2
        );
    }
}
