package logic;
import logic.rules.*;
import model.Piece;
import model.Position;
import java.util.HashMap;
import java.util.Map;

public class RulesEngine {
    private Map<Character, MoveRule> rules = new HashMap<>();


      public RulesEngine() {
            rules.put('P', new PawnRule());
            rules.put('Q', new QueenRule());
            rules.put('R', new RookRules());
            rules.put('N', new KnightRule());
            rules.put('B', new BishopRule());
            rules.put('K', new KingRule());
        }


    public boolean isMoveLegal(Piece[][] grid, Position from, Position to) {
        Piece piece = grid[from.getRow()][from.getCol()];
        if (piece == null) return false;

        MoveRule rule = rules.get(piece.getType());
        if (rule != null) {
           
            return MoveValidator.isLegalMove(grid, from, to, rule);
        }
        return false;
    }
}