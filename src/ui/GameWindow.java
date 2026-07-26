package ui;

import engine.GameEngine;
import engine.GameController;
import engine.GameSnapshot;
import image.Img;
import view.ImgRenderer;
import engine.StateConfigLoader;

import javax.swing.*;
import java.awt.*;

public class GameWindow extends JPanel {
    private static final int CELL_SIZE = 100;
    private static final int FRAME_DELAY_MS = 16;

    private final GameEngine engine;
    private final GameController controller;
    private final ImgRenderer renderer;
    private final StateConfigLoader configLoader;

    private JLabel imageLabel;
    private JLabel overlayLabel;
    private Timer timer;
    private long previousTimeNanos;

    public GameWindow(GameEngine engine, GameController controller, ImgRenderer renderer, StateConfigLoader configLoader) {
        this.engine = engine;
        this.controller = controller;
        this.renderer = renderer;
        this.configLoader = configLoader;

        setLayout(new BorderLayout());
        imageLabel = new JLabel();
        imageLabel.setFocusable(true);

        overlayLabel = new JLabel("", JLabel.CENTER);
        overlayLabel.setOpaque(true);
        overlayLabel.setBackground(Color.YELLOW);
        overlayLabel.setForeground(Color.BLACK);
        overlayLabel.setVisible(false);

        imageLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent event) {
                controller.click(event.getX(), event.getY());
            }
        });

        add(imageLabel, BorderLayout.CENTER);
        add(overlayLabel, BorderLayout.NORTH);

        renderCurrentState();
    }

    public void startGameLoop() {
        previousTimeNanos = System.nanoTime();

        timer = new Timer(FRAME_DELAY_MS, event -> updateFrame());
        timer.start();
    }

    public void stopGameLoop() {
        if (timer != null) {
            timer.stop();
        }
    }

    private void updateFrame() {
        long now = System.nanoTime();
        long elapsedMillis = (now - previousTimeNanos) / 1_000_000;
        previousTimeNanos = now;

        // קדם את הזמן במנוע המשחק
        engine.getRealTimeManager().advanceClock(elapsedMillis, engine.getBoard(), engine);

        renderCurrentState();
    }

    private void renderCurrentState() {
        GameSnapshot snapshot = engine.snapshot(controller.getSelectedPosition());

        Img rendered = renderer.render(snapshot);

        imageLabel.setIcon(new ImageIcon(rendered.get()));
        imageLabel.revalidate();
        imageLabel.repaint();
    }

    public void showOverlay(String text) {
        System.out.println("[GameWindow] showOverlay: " + text);
        overlayLabel.setText(text);
        overlayLabel.setVisible(true);
        revalidate();
        repaint();
    }

    public void clearOverlay() {
        System.out.println("[GameWindow] clearOverlay");
        overlayLabel.setVisible(false);
        revalidate();
        repaint();
    }
}
