package logic.rules;


import model.Piece;
import model.Position;

public interface MoveRule {
    boolean isLegal(Piece[][] grid, Position from, Position to);
}