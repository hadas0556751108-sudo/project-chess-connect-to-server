package view;

import engine.PieceSnapshot;
import engine.PieceVisualState;
import engine.StateConfigLoader;
import engine.StateConfig;
import image.Img;

import java.awt.Dimension;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PieceImageLoader {
    private final String rootPath;
    private final int cellSize;
    private final StateConfigLoader configLoader;
    private final Map<ImageKey, List<Img>> frames = new HashMap<>();

    public PieceImageLoader(String rootPath, int cellSize, StateConfigLoader configLoader) {
        this.rootPath = rootPath;
        this.cellSize = cellSize;
        this.configLoader = configLoader;
        preloadAllImages();
    }

    public Img getFrame(PieceSnapshot piece) {
        ImageKey key = new ImageKey(
                piece.type(),
                piece.color(),
                piece.state()
        );

        List<Img> animationFrames = frames.get(key);

        // אם אין frames למצב המבוקש, נסה להשתמש ב-IDLE
        if (animationFrames == null || animationFrames.isEmpty()) {
            ImageKey idleKey = new ImageKey(
                    piece.type(),
                    piece.color(),
                    PieceVisualState.IDLE
            );
            animationFrames = frames.get(idleKey);
        }

        if (animationFrames == null || animationFrames.isEmpty()) {
            throw new IllegalStateException("Missing frames for " + key + " (and IDLE fallback)");
        }

        int frameIndex = calculateFrameIndex(
                piece.stateElapsedMillis(),
                animationFrames.size(),
                piece.type(),
                piece.color(),
                piece.state()
        );

        return animationFrames.get(frameIndex).copy();
    }

    private int calculateFrameIndex(long elapsedMillis, int frameCount,
                                    char type, char color, PieceVisualState state) {
        String pieceKey = Character.toLowerCase(color) + "" + type;
        String stateName = state.toString().toLowerCase();
        
        StateConfig config = configLoader.getConfig(pieceKey, stateName);
        int framesPerSec = (config != null) ? config.framesPerSec() : 6; // ברירת מחדל 6 fps
        
        int frameDurationMillis = 1000 / framesPerSec;
        long rawIndex = elapsedMillis / frameDurationMillis;

        return (int) (rawIndex % frameCount);
    }

    private void preloadAllImages() {
        char[] types = {'P', 'R', 'N', 'B', 'Q', 'K'};
        char[] colors = {'W', 'B'};
        PieceVisualState[] states = PieceVisualState.values();

        for (char type : types) {
            for (char color : colors) {
                for (PieceVisualState state : states) {
                    loadAnimation(type, color, state);
                }
            }
        }
    }

    private String pieceFolder(char type, char color) {
        return Character.toLowerCase(color) + "" + type;
    }

    private String stateFolder(PieceVisualState state) {
        return state.toString().toLowerCase();
    }

    private void loadAnimation(char type, char color, PieceVisualState state) {
        String path = rootPath + "/" + pieceFolder(type, color) + "/states/" + stateFolder(state) + "/sprites";

        List<Img> loadedFrames = new ArrayList<>();

        for (int i = 1; ; i++) {
            File file = new File(path + "/" + i + ".png");

            if (!file.exists()) {
                break;
            }

            Img frame = new Img().read(
                    file.getPath(),
                    new Dimension(cellSize, cellSize),
                    true,
                    null
            );

            loadedFrames.add(frame);
        }

        if (!loadedFrames.isEmpty()) {
            frames.put(
                    new ImageKey(type, color, state),
                    loadedFrames
            );
        }
    }

    private static class ImageKey {
        private final char type;
        private final char color;
        private final PieceVisualState state;

        public ImageKey(char type, char color, PieceVisualState state) {
            this.type = type;
            this.color = color;
            this.state = state;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ImageKey)) return false;
            ImageKey imageKey = (ImageKey) o;
            return type == imageKey.type &&
                    color == imageKey.color &&
                    state == imageKey.state;
        }

        @Override
        public int hashCode() {
            return 31 * (31 * type + color) + state.hashCode();
        }

        @Override
        public String toString() {
            return "" + type + color + "_" + state;
        }
    }
}
