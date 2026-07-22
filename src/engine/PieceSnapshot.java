package engine;

public class PieceSnapshot {
    private final String id;
    private final char type;
    private final char color;
    private final PieceVisualState state;
    private final double pixelX;
    private final double pixelY;
    private final long stateElapsedMillis;

    public PieceSnapshot(String id, char type, char color, PieceVisualState state,
                       double pixelX, double pixelY, long stateElapsedMillis) {
        this.id = id;
        this.type = type;
        this.color = color;
        this.state = state;
        this.pixelX = pixelX;
        this.pixelY = pixelY;
        this.stateElapsedMillis = stateElapsedMillis;
    }

    public String id() { return id; }
    public char type() { return type; }
    public char color() { return color; }
    public PieceVisualState state() { return state; }
    public double pixelX() { return pixelX; }
    public double pixelY() { return pixelY; }
    public long stateElapsedMillis() { return stateElapsedMillis; }
}
