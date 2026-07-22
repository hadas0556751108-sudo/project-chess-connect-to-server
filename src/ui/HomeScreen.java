package ui;

import engine.GameController;
import engine.GameEngine;
import engine.RealTimeManager;
import engine.StateConfigLoader;
import input.BoardMapper;
import model.Board;
import model.User;
import network.ChessClient;
import network.MessageHandler;
import network.Protocol;
import view.ImgRenderer;
import view.PieceImageLoader;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class HomeScreen extends JFrame implements MessageHandler {
    private final ChessClient client;
    private final String username;
    private final User user;
    private final Runnable onClose;

    private final JTextArea messageArea = new JTextArea(16, 50);
    private final JTextField inputField = new JTextField(20);
    private final JLabel statusLabel = new JLabel("Not in a room");
    private final JLabel countdownLabel = new JLabel("");

    private volatile int currentRoomId = -1;
    private volatile String currentColor = null;
    private volatile boolean gameStarted = false;
    private volatile boolean matchmaking = false;
    private volatile String whiteUsername = null;
    private volatile String blackUsername = null;
    private volatile boolean gameOverDialogShown = false;

    private GameWindow gameWindow;
    private JFrame boardFrame;
    private GameController gameController;

    private final JButton playButton = new JButton("Play (Matchmaking)");
    private final JButton resignButton = new JButton("Resign");
    private final JButton drawButton = new JButton("Draw");

    public HomeScreen(ChessClient client, User user, Runnable onClose) {
        this.client = client;
        this.username = user.getUsername();
        this.user = user;
        this.onClose = onClose;

        setTitle("Chess Server - Welcome " + username + " (ELO: " + user.getElo() + ")");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        initComponents();
        pack();
        setLocationRelativeTo(null);

        client.addMessageHandler(this);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                close();
            }
        });
    }

    private void initComponents() {
        messageArea.setEditable(false);
        messageArea.setLineWrap(true);
        JScrollPane scrollPane = new JScrollPane(messageArea);

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(new JLabel("Player: " + username));
        topPanel.add(statusLabel);
        topPanel.add(countdownLabel);

        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        inputPanel.add(new JLabel("Room id / name:"));
        inputPanel.add(inputField);

        JPanel buttonPanel = new JPanel(new GridLayout(0, 3, 5, 5));
        JButton createButton = new JButton("Create Room");
        JButton joinButton = new JButton("Join Room");
        JButton leaveButton = new JButton("Leave Room");
        JButton listButton = new JButton("List Rooms");
        JButton exitButton = new JButton("Exit");

        createButton.addActionListener(e -> createRoom());
        joinButton.addActionListener(e -> joinRoom());
        leaveButton.addActionListener(e -> leaveRoom());
        listButton.addActionListener(e -> client.sendCommand(Protocol.LIST_ROOMS));
        playButton.addActionListener(e -> togglePlay());
        resignButton.addActionListener(e -> client.sendCommand(Protocol.RESIGN));
        drawButton.addActionListener(e -> client.sendCommand(Protocol.GAME_OVER + " DRAW"));
        exitButton.addActionListener(e -> close());

        buttonPanel.add(createButton);
        buttonPanel.add(joinButton);
        buttonPanel.add(leaveButton);
        buttonPanel.add(listButton);
        buttonPanel.add(playButton);
        buttonPanel.add(resignButton);
        buttonPanel.add(drawButton);
        buttonPanel.add(exitButton);

        setLayout(new BorderLayout(5, 5));
        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        add(inputPanel, BorderLayout.EAST);
    }

    private void createRoom() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) {
            client.sendCommand(Protocol.CREATE_ROOM);
        } else {
            client.sendCommand(Protocol.CREATE_ROOM + " " + text);
        }
    }

    private void joinRoom() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) {
            appendMessage("Please enter a room id.");
            return;
        }
        client.sendCommand(Protocol.JOIN_ROOM + " " + text);
    }

    private void leaveRoom() {
        client.sendCommand(Protocol.LEAVE_ROOM);
    }

    private void togglePlay() {
        if (matchmaking) {
            client.sendCommand(Protocol.CANCEL_PLAY);
            matchmaking = false;
            playButton.setText("Play (Matchmaking)");
            statusLabel.setText("Not in a room");
        } else {
            client.sendCommand(Protocol.PLAY);
            matchmaking = true;
            playButton.setText("Cancel Matchmaking");
            statusLabel.setText("Matchmaking...");
        }
    }

    @Override
    public void handleMessage(String message) {
        SwingUtilities.invokeLater(() -> handleServerMessage(message));
    }

    private void handleServerMessage(String message) {
        appendMessage("SERVER: " + message);
        if (message == null) {
            return;
        }
        try {
            String[] parts = message.trim().split("\\s+");
            String type = parts[0];

            if (Protocol.ROOM_CREATED.equals(type) && parts.length > 1) {
                parseRoomJoined(Integer.parseInt(parts[1]), "WHITE");
            } else if (Protocol.JOINED_ROOM.equals(type) && parts.length > 2) {
                parseRoomJoined(Integer.parseInt(parts[1]), parts[2]);
            } else if (Protocol.LEFT_ROOM.equals(type) && parts.length > 1) {
                parseRoomLeft(Integer.parseInt(parts[1]));
            } else if (Protocol.ROOM_CLOSED.equals(type) && parts.length > 1) {
                parseRoomLeft(Integer.parseInt(parts[1]));
                JOptionPane.showMessageDialog(this, "Room closed.");
            } else if (Protocol.GAME_START.equals(type) && parts.length > 1) {
                gameStarted = true;
                gameOverDialogShown = false;
                statusLabel.setText("Game started in room " + currentRoomId + " (" + currentColor + ")");
                if (parts.length > 3) {
                    whiteUsername = parts[2];
                    blackUsername = parts[3];
                }
                int startRoomId = Integer.parseInt(parts[1]);
                if (startRoomId == currentRoomId && currentColor != null) {
                    openGameWindow();
                } else {
                    System.out.println("[HomeScreen] GAME_START roomId=" + startRoomId + " current=" + currentRoomId + "/" + currentColor + "; waiting");
                }
            } else if (Protocol.NO_MATCH.equals(type)) {
                matchmaking = false;
                playButton.setText("Play (Matchmaking)");
                statusLabel.setText("No match found");
                JOptionPane.showMessageDialog(this, "No match found. Try again later.");
            } else if (Protocol.OPPONENT_DISCONNECTED.equals(type) && parts.length > 2) {
                statusLabel.setText("Opponent disconnected from room " + parts[1]);
                if (gameWindow != null) {
                    gameWindow.showOverlay("Opponent disconnected");
                }
            } else if (Protocol.OPPONENT_RECONNECTED.equals(type) && parts.length > 2) {
                countdownLabel.setText("");
                statusLabel.setText("Opponent reconnected to room " + parts[1]);
                if (gameWindow != null) {
                    gameWindow.clearOverlay();
                }
            } else if (Protocol.DISCONNECT_COUNTDOWN.equals(type) && parts.length > 3) {
                countdownLabel.setText("Reconnect in " + parts[2] + "s");
                if (gameWindow != null) {
                    gameWindow.showOverlay("Reconnect in " + parts[2] + "s");
                }
            } else if (Protocol.ROOM_LIST.equals(type)) {
                statusLabel.setText("Rooms updated");
            } else if (Protocol.GAME_OVER.equals(type) && parts.length > 1) {
                handleGameOver(parts[1]);
            } else if (Protocol.ELO_UPDATE.equals(type) && parts.length > 2 && username.equals(parts[1])) {
                try {
                    int newElo = Integer.parseInt(parts[2]);
                    user.setElo(newElo);
                    setTitle("Chess Server - Welcome " + username + " (ELO: " + newElo + ")");
                } catch (NumberFormatException e) {
                    System.err.println("[HomeScreen] Invalid ELO_UPDATE: " + message);
                }
            } else if (Protocol.SOUND.equals(type) && parts.length > 1) {
                SoundPlayer.play(parts[1]);
            }
        } catch (NumberFormatException e) {
            System.err.println("[HomeScreen] Invalid numeric value in message: " + message);
        }
    }

    private void parseRoomJoined(int roomId, String color) {
        currentRoomId = roomId;
        currentColor = color;
        gameOverDialogShown = false;
        matchmaking = false;
        playButton.setText("Play (Matchmaking)");
        statusLabel.setText("In room " + currentRoomId + " as " + currentColor);
        countdownLabel.setText("");
        if (gameStarted) {
            System.out.println("[HomeScreen] color arrived after GAME_START; opening board");
            openGameWindow();
        }
    }

    private void parseRoomLeft(int roomId) {
        if (roomId == currentRoomId) {
            currentRoomId = -1;
            currentColor = null;
            gameStarted = false;
            matchmaking = false;
            playButton.setText("Play (Matchmaking)");
            statusLabel.setText("Not in a room");
            countdownLabel.setText("");
            closeGameWindow();
        }
    }

    private void appendMessage(String text) {
        messageArea.append(text + "\n");
        messageArea.setCaretPosition(messageArea.getDocument().getLength());
    }

    private void close() {
        if (matchmaking) {
            client.sendCommand(Protocol.CANCEL_PLAY);
        }
        if (currentRoomId >= 0) {
            client.sendCommand(Protocol.LEAVE_ROOM);
        }
        closeGameWindow();
        setVisible(false);
        dispose();
        if (onClose != null) {
            onClose.run();
        }
    }

    private void openGameWindow() {
        closeGameWindow();

        System.out.println("[HomeScreen] openGameWindow: roomId=" + currentRoomId + " color=" + currentColor);

        String[][] initialPosition = {
                {"BR", "BN", "BB", "BQ", "BK", "BB", "BN", "BR"},
                {"BP", "BP", "BP", "BP", "BP", "BP", "BP", "BP"},
                {".", ".", ".", ".", ".", ".", ".", "."},
                {".", ".", ".", ".", ".", ".", ".", "."},
                {".", ".", ".", ".", ".", ".", ".", "."},
                {".", ".", ".", ".", ".", ".", ".", "."},
                {"WP", "WP", "WP", "WP", "WP", "WP", "WP", "WP"},
                {"WR", "WN", "WB", "WQ", "WK", "WB", "WN", "WR"}
        };

        Board board = new Board(initialPosition);
        RealTimeManager timeManager = new RealTimeManager();
        StateConfigLoader configLoader = new StateConfigLoader("assets/pieces_mine", 100.0);
        GameEngine engine = new GameEngine(board, timeManager, configLoader);
        BoardMapper boardMapper = new BoardMapper(100, 8, 8);

        gameController = new GameController(engine, boardMapper, client, currentColor, this::handleGameOver);

        PieceImageLoader imageLoader = new PieceImageLoader("assets/pieces_mine", 100, configLoader);
        ImgRenderer renderer = new ImgRenderer("assets/board.png", 100, imageLoader);
        gameWindow = new GameWindow(engine, gameController, renderer, configLoader);

        boardFrame = new JFrame("Chess - Room " + currentRoomId + " - " + currentColor);
        boardFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        boardFrame.setSize(800, 800);
        boardFrame.add(gameWindow);
        boardFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                closeGameWindow();
            }
        });
        boardFrame.setVisible(true);
        gameWindow.startGameLoop();
    }

    private void closeGameWindow() {
        if (gameController != null) {
            client.removeMessageHandler(gameController);
            gameController = null;
        }
        if (gameWindow != null) {
            gameWindow.stopGameLoop();
            gameWindow = null;
        }
        if (boardFrame != null) {
            boardFrame.dispose();
            boardFrame = null;
        }
    }

    private void handleGameOver(String result) {
        if (gameOverDialogShown) {
            return;
        }
        gameOverDialogShown = true;
        gameStarted = false;
        statusLabel.setText("Game over in room " + currentRoomId);
        String winnerName = winnerNameFromResult(result);
        String message = "DRAW".equals(result) ? winnerName : winnerName + " wins";
        SoundPlayer.play("gameover");
        Component dialogParent = boardFrame != null ? boardFrame : this;
        JOptionPane.showMessageDialog(dialogParent, message);
        closeGameWindow();
    }

    private String winnerNameFromResult(String result) {
        if (result == null || "DRAW".equals(result)) {
            return "Draw";
        }
        if ("WHITE".equals(result)) {
            return whiteUsername != null ? whiteUsername : "White";
        }
        if ("BLACK".equals(result)) {
            return blackUsername != null ? blackUsername : "Black";
        }
        return result;
    }
}
