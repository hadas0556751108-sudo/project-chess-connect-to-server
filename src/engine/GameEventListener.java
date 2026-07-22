package engine;

import model.Position;

public interface GameEventListener {
    void onMoveRequested(Position from, Position to);
    void onMoveExecuted(Position from, Position to);
    void onGameOver(String winner);
}
