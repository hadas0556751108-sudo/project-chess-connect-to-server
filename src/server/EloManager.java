package server;

import auth.AuthManager;
import model.User;

import java.sql.SQLException;

public class EloManager {
    private final AuthManager authManager;

    public EloManager(AuthManager authManager) {
        this.authManager = authManager;
    }

    public void updateRatings(String white, String black, String result) throws SQLException {
        User whiteUser = authManager.getUser(white);
        User blackUser = authManager.getUser(black);
        if (whiteUser == null || blackUser == null) {
            return;
        }

        double whiteScore;
        double blackScore;
        if ("WHITE".equals(result)) {
            whiteScore = 1.0;
            blackScore = 0.0;
        } else if ("BLACK".equals(result)) {
            whiteScore = 0.0;
            blackScore = 1.0;
        } else if ("DRAW".equals(result)) {
            whiteScore = 0.5;
            blackScore = 0.5;
        } else {
            return;
        }

        int k = 32;
        int whiteRating = whiteUser.getElo();
        int blackRating = blackUser.getElo();
        double expectedWhite = expectedScore(whiteRating, blackRating);
        double expectedBlack = expectedScore(blackRating, whiteRating);

        int newWhiteRating = (int) Math.round(whiteRating + k * (whiteScore - expectedWhite));
        int newBlackRating = (int) Math.round(blackRating + k * (blackScore - expectedBlack));

        authManager.updateElo(white, newWhiteRating);
        authManager.updateElo(black, newBlackRating);
    }

    private double expectedScore(int ratingA, int ratingB) {
        return 1.0 / (1.0 + Math.pow(10.0, (ratingB - ratingA) / 400.0));
    }
}
