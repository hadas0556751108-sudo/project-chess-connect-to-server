package model;

import engine.PieceVisualState;



public class Piece {
    private char color;
    private char type;
    private boolean hasMoved=false;
    public enum State { IDLE, MOVING, JUMPING, SHORT_REST, LONG_REST }
    private State currentState = State.IDLE;
    private long stateStartTime = 0; 

    public Piece(char color, char type) {
        this.color = color;
        this.type = type;
        this.hasMoved = false;
        this.stateStartTime = 0; 
    }

   
    public char getColor() { return color; }
    public char getType() { return type; }
    public boolean hasMoved() { return hasMoved; }
    public void setHasMoved(boolean hasMoved) { this.hasMoved = hasMoved; }
    public State getCurrentState() { return currentState; }
    public void setCurrentState(State state) { 
        this.currentState = state; 
       
    }
    public void setStateStartTime(long time) {
        this.stateStartTime = time;
    }
    public long getStateStartTime() { return stateStartTime; }
    public long getStateElapsedMillis(long currentGameClock) { 
        return currentGameClock - stateStartTime; 
    }

    @Override
    public String toString() {
        return "" + color + type;
    }
    
    public PieceVisualState getVisualState() {
        switch (this.currentState) {
            case IDLE: return PieceVisualState.IDLE;
            case MOVING: return PieceVisualState.MOVING;
            case JUMPING: return PieceVisualState.JUMPING;
            case SHORT_REST: return PieceVisualState.SHORT_REST;
            case LONG_REST: return PieceVisualState.LONG_REST;
            default: return PieceVisualState.IDLE;
        }
    }

    public Piece copy() {
        Piece copy = new Piece(this.color, this.type);
        copy.setHasMoved(this.hasMoved);
        copy.setCurrentState(this.currentState);
        copy.setStateStartTime(this.stateStartTime);
        return copy;
    }
}
