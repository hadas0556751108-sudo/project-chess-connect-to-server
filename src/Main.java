import engine.GameController;
import engine.GameEngine;
import engine.RealTimeManager;
import engine.StateConfigLoader;
import input.BoardMapper;
import model.Board;
import view.ImgRenderer;
import view.PieceImageLoader;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        Board myBoard = createInitialBoard();

        RealTimeManager timeManager = new RealTimeManager();
        StateConfigLoader configLoader = new StateConfigLoader("assets/pieces_mine", 100.0);
        GameEngine engine = new GameEngine(myBoard, timeManager, configLoader);

        int cellSize = 100;
        BoardMapper boardMapper = new BoardMapper(cellSize, 8, 8);

        PieceImageLoader imageLoader = new PieceImageLoader(
                "assets/pieces_mine",
                cellSize,
                configLoader
        );

        ImgRenderer renderer = new ImgRenderer(
                "assets/board.png",
                cellSize,
                imageLoader
        );

        GameController controller = new GameController(engine, boardMapper);

        JFrame frame = new JFrame("שחמט - קונג פו");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 800);

        ui.GameWindow gamePanel = new ui.GameWindow(engine, controller, renderer, configLoader);

        frame.add(gamePanel);
        frame.setVisible(true);

        gamePanel.startGameLoop();

    }
    private static Board createInitialBoard() {
        String[][] initialPosition = {
                {"BR", "BN", "BB", "BQ", "BK", "BB", "BN", "BR"},  // שורה 0 - כלים שחורים
                {"BP", "BP", "BP", "BP", "BP", "BP", "BP", "BP"},  // שורה 1 - חיילים שחורים
                {".", ".", ".", ".", ".", ".", ".", "."},           // שורה 2 - ריק
                {".", ".", ".", ".", ".", ".", ".", "."},           // שורה 3 - ריק
                {".", ".", ".", ".", ".", ".", ".", "."},           // שורה 4 - ריק
                {".", ".", ".", ".", ".", ".", ".", "."},           // שורה 5 - ריק
                {"WP", "WP", "WP", "WP", "WP", "WP", "WP", "WP"},  // שורה 6 - חיילים לבנים
                {"WR", "WN", "WB", "WQ", "WK", "WB", "WN", "WR"}   // שורה 7 - כלים לבנים
        };

        return new Board(initialPosition);
    }
}
