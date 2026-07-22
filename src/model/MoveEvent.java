package model;

public class MoveEvent {
    private final Piece piece;
    private final Position from;
    private final Position to;
    private final long arrivalTime;
    private final boolean isJump;

    public MoveEvent(Piece piece, Position from, Position to, long arrivalTime) {
        this(piece, from, to, arrivalTime, false);
    }

    public MoveEvent(Piece piece, Position from, Position to, long arrivalTime, boolean isJump) {
        this.piece = piece;
        this.from = from;
        this.to = to;
        this.arrivalTime = arrivalTime;
        this.isJump = isJump;
    }

    public Piece getPiece() { return piece; }
    public Position getFrom() { return from; }
    public Position getTo() { return to; }
    public long getArrivalTime() { return arrivalTime; }
    public boolean isJump() { return isJump; }
}