package app;

import auth.AuthManager;
import auth.LoginCLI;
import database.UserRepository;
import model.User;
import network.ChessClient;
import server.ChessServer;
import server.CommandRouter;
import ui.HomeScreen;

import javax.swing.*;
import java.net.URI;
import java.sql.SQLException;
import java.util.Scanner;

public class SingleProcessLauncher {
    public static void main(String[] args) throws Exception {
        String dbPath = "chess.db";
        UserRepository repository = new UserRepository(dbPath);
        try {
            repository.init();
        } catch (SQLException e) {
            System.err.println("Failed to initialize database: " + e.getMessage());
            return;
        }

        AuthManager authManager = new AuthManager(repository);
        CommandRouter router = new CommandRouter(authManager);
        ChessServer server = new ChessServer(8080, router);
        server.start();

        // Allow the server to start before the client connects.
        Thread.sleep(800);

        ChessClient client = new ChessClient(new URI("ws://localhost:8080"));
        client.connectBlocking();

        LoginCLI cli = new LoginCLI(client, new Scanner(System.in));
        User user = cli.run();

        if (user == null) {
            System.out.println("Login cancelled. Closing application.");
            client.close();
            server.stop();
            return;
        }

        SwingUtilities.invokeLater(() -> {
            HomeScreen screen = new HomeScreen(client, user, () -> {
                try {
                    client.close();
                    server.stop();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                System.exit(0);
            });
            screen.setVisible(true);
        });
    }
}
