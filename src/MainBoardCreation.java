//import engine.GameController;
//import engine.GameEngine;
//import engine.RealTimeManager;
//import model.Board;
//import ui.GameWindow;
//
//import javax.swing.*;
//
//public class MainBoardCreation {
//    public static void main(String[] args) {
//        // יצירת לוח חדש
//        Board myBoard = createInitialBoard();
//
//        // יצירת מערכות העזר
//        RealTimeManager timeManager = new RealTimeManager();
//        GameEngine engine = new GameEngine(myBoard, timeManager);
//        GameController controller = new GameController(engine);
//
//        // יצירת חלון המשחק
//        JFrame frame = new JFrame("שחמט - קונג פו");
//        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        frame.setSize(800, 800);
//
//        GameWindow gamePanel = new GameWindow(myBoard, controller);
//        frame.add(gamePanel);
//        frame.setVisible(true);
//    }
//
//    /**
//     * יוצר לוח שחמט עם כל הכלים בעמדה התחלתית
//     */
//    private static Board createInitialBoard() {
//        String[][] initialPosition = {
//                {"BR", "BN", "BB", "BQ", "BK", "BB", "BN", "BR"},  // שורה 0 - כלים שחורים
//                {"BP", "BP", "BP", "BP", "BP", "BP", "BP", "BP"},  // שורה 1 - חיילים שחורים
//                {".", ".", ".", ".", ".", ".", ".", "."},           // שורה 2 - ריק
//                {".", ".", ".", ".", ".", ".", ".", "."},           // שורה 3 - ריק
//                {".", ".", ".", ".", ".", ".", ".", "."},           // שורה 4 - ריק
//                {".", ".", ".", ".", ".", ".", ".", "."},           // שורה 5 - ריק
//                {"WP", "WP", "WP", "WP", "WP", "WP", "WP", "WP"},  // שורה 6 - חיילים לבנים
//                {"WR", "WN", "WB", "WQ", "WK", "WB", "WN", "WR"}   // שורה 7 - כלים לבנים
//        };
//
//        return new Board(initialPosition);
//    }
//}
