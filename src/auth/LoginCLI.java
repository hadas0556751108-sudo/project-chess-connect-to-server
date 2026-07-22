package auth;

import model.User;
import network.ChessClient;
import network.Protocol;

import java.util.Scanner;

public class LoginCLI {
    private final ChessClient client;
    private final Scanner scanner;

    public LoginCLI(ChessClient client, Scanner scanner) {
        this.client = client;
        this.scanner = scanner;
    }

    public User run() {
        System.out.println("Welcome to Chess Server");
        System.out.println("Commands: REGISTER <username> <password> | LOGIN <username> <password> | QUIT");

        while (true) {
            System.out.print("> ");
            String line;
            try {
                line = scanner.nextLine();
            } catch (Exception e) {
                System.err.println("Input closed.");
                return null;
            }
            if (line == null || line.trim().isEmpty()) {
                continue;
            }

            String[] parts = line.trim().split("\\s+");
            String command = parts[0].toUpperCase();

            if (command.equals("QUIT") || command.equals("EXIT")) {
                return null;
            }

            if (command.equals(Protocol.REGISTER) || command.equals(Protocol.LOGIN)) {
                if (parts.length != 3) {
                    System.out.println("Usage: " + command + " <username> <password>");
                    continue;
                }
                client.sendCommand(command + " " + parts[1] + " " + parts[2]);
                try {
                    String response = client.nextMessage(5000);
                    if (response != null) {
                        System.out.println(response);
                        if (response.startsWith(Protocol.LOGIN_SUCCESS)) {
                            return parseUser(response);
                        } else if (response.startsWith(Protocol.REGISTER_SUCCESS)) {
                            System.out.println("Registered successfully. You can now LOGIN.");
                        }
                    } else {
                        System.out.println("No response from server");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            } else {
                System.out.println("Unknown command: " + command);
            }
        }
    }

    private User parseUser(String response) {
        String[] parts = response.split("\\s+");
        String username = parts.length > 1 ? parts[1] : "unknown";
        int elo = 1200;
        if (parts.length > 2) {
            try {
                elo = Integer.parseInt(parts[2]);
            } catch (NumberFormatException ignored) {
            }
        }
        return new User(username, "", elo);
    }
}
