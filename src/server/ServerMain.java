package server;

import auth.AuthManager;
import database.UserRepository;

import java.sql.SQLException;

public class ServerMain {
    public static void main(String[] args) throws Exception {
        String dbPath = args.length > 0 ? args[0] : "chess.db";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 8080;

        UserRepository repository = new UserRepository(dbPath);
        try {
            repository.init();
        } catch (SQLException e) {
            System.err.println("Failed to initialize database: " + e.getMessage());
            return;
        }

        AuthManager authManager = new AuthManager(repository);
        CommandRouter router = new CommandRouter(authManager);
        ChessServer server = new ChessServer(port, router);
        server.start();

        System.out.println("Press Enter to stop the server...");
        System.in.read();

        server.stop();
    }
}
