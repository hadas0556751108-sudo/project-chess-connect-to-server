package client;

import auth.LoginCLI;
import model.User;
import network.ChessClient;
import network.ClientLogger;
import ui.HomeScreen;

import javax.swing.*;
import java.net.URI;
import java.util.Scanner;

public class ClientMain {
    public static void main(String[] args) throws Exception {
        ClientLogger.getInstance().redirectSystemStreams();

        String uri = args.length > 0 ? args[0] : "ws://localhost:8080";

        ChessClient client = new ChessClient(new URI(uri));
        client.connectBlocking();

        LoginCLI cli = new LoginCLI(client, new Scanner(System.in));
        User user = cli.run();

        if (user == null) {
            client.close();
            return;
        }

        ClientLogger.getInstance().setUsername(user.getUsername());

        final ChessClient finalClient = client;
        SwingUtilities.invokeLater(() -> {
            HomeScreen screen = new HomeScreen(finalClient, user, () -> {
                finalClient.close();
                System.exit(0);
            });
            screen.setVisible(true);
        });
    }
}
