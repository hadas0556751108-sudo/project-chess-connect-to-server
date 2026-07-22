package ui;

import java.util.HashMap;
import java.util.Map;

/**
 * Config פשוט לכל piece state.
 * ברירת מחדל: offset = (0,0), frames=6
 */
public class SpriteConfig {
    private final int offsetX;
    private final int offsetY;
    private final int framesPerSec;

    private static final Map<String, SpriteConfig> cache = new HashMap<>();

    public SpriteConfig(int offsetX, int offsetY, int framesPerSec) {
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.framesPerSec = framesPerSec;
    }

    public static SpriteConfig getDefault() {
        return new SpriteConfig(0, 0, 6);
    }

    public int getOffsetX() { return offsetX; }
    public int getOffsetY() { return offsetY; }
    public int getFramesPerSec() { return framesPerSec; }
}
