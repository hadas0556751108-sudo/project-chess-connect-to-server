package ui;

import model.Piece;
import model.Piece.State;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;

public class SpriteManager {
    private Map<String, List<BufferedImage>> sprites = new HashMap<>();

    public SpriteManager() {
        loadAllSprites();
    }

    private void loadAllSprites() {
        String[] pieces = {"PW", "RW", "NW", "BW", "QW", "KW", "PB", "RB", "NB", "BB", "QB", "KB"};
       // State[] states = {State.IDLE, State.WALKING, State.JUMPING};

        for (String p : pieces) {
            for (Piece.State s : Piece.State.values()) {
                List<BufferedImage> frames = new ArrayList<>();
                // כאן התיקון הקריטי: המרת שם המצב לאותיות קטנות כדי להתאים לנתיב התיקייה
                String stateFolderName = s.toString().toLowerCase();

                for (int i = 1; i <= 5; i++) {
                    File file = null;
                    try {
                        // בניית הנתיב הדינמי:
                        file = new File("resources/pieces2/" + p + "/states/" + stateFolderName + "/sprites/" + i + ".png");

                        if (file.exists()) {
                            frames.add(ImageIO.read(file));
                        }
                    } catch (java.io.IOException e) {
                        System.err.println("שגיאה בטעינת הקובץ: " + file.getPath());
                    }
                }
                // שמירה במפה עם מפתח שמכיל גם את הכלי וגם את המצב
                if (!frames.isEmpty()) {
                    sprites.put(p + "_" + s.toString(), frames);
                }
            }
        }
    }

    public BufferedImage getFrame(String pieceKey, State state, int frameIndex) {
        // שליפה לפי המפתח המשולב (למשל: "PW_WALKING")
        List<BufferedImage> frames = sprites.get(pieceKey + "_" + state.toString());

        if (frames == null || frames.isEmpty()) return null;

        // אם זה IDLE, נחזיר תמיד פריים 0 כדי למנוע אנימציה רצה במקום
        if (state == State.IDLE) return frames.get(0);

        // אחרת, נחזיר את הפריים לפי האנימציה
        return frames.get(frameIndex % frames.size());
    }
}