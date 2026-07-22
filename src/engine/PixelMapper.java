package engine;

import model.Position;
public class PixelMapper {
    public static Position pixelsToPosition(int x, int y) {

        return new Position(y / 100, x / 100);
    }
}