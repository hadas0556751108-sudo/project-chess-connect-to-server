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
                {"BR", "BN", "BB", "BQ", "BK", "BB", "BN", "BR"},  
                {"BP", "BP", "BP", "BP", "BP", "BP", "BP", "BP"},  
                {".", ".", ".", ".", ".", ".", ".", "."},           
                {".", ".", ".", ".", ".", ".", ".", "."},           
                {".", ".", ".", ".", ".", ".", ".", "."},           
                {".", ".", ".", ".", ".", ".", ".", "."},           
                {"WP", "WP", "WP", "WP", "WP", "WP", "WP", "WP"},  
                {"WR", "WN", "WB", "WQ", "WK", "WB", "WN", "WR"}  
        };

        return new Board(initialPosition);
    }
}
