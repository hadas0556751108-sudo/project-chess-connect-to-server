package ui;

import javafx.embed.swing.JFXPanel;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import javax.swing.SwingUtilities;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SoundPlayer {
    private static final Map<String, String> EFFECT_FILES = new HashMap<>();
    private static final Map<String, Media> MEDIA_CACHE = new HashMap<>();
    private static final Set<MediaPlayer> ACTIVE_PLAYERS = Collections.synchronizedSet(new HashSet<>());
    private static final File SOUND_DIR = new File(System.getProperty("user.dir"), "sound");
    private static JFXPanel jfxPanel;

    static {
        EFFECT_FILES.put("move", "move.wav");
        EFFECT_FILES.put("eat", "eat.mp3");
        EFFECT_FILES.put("gameover", "game over.mp3");
        EFFECT_FILES.put("click", "click.mp3");
    }

    public static void play(String effect) {
        String filename = EFFECT_FILES.get(effect);
        if (filename == null) {
            return;
        }
        ensureJfx();
        File file = new File(SOUND_DIR, filename);
        if (!file.exists()) {
            return;
        }
        try {
            Media media = MEDIA_CACHE.get(effect);
            if (media == null) {
                media = new Media(file.toURI().toString());
                MEDIA_CACHE.put(effect, media);
            }
            MediaPlayer player = new MediaPlayer(media);
            player.setOnEndOfMedia(() -> ACTIVE_PLAYERS.remove(player));
            player.setOnError(() -> ACTIVE_PLAYERS.remove(player));
            ACTIVE_PLAYERS.add(player);
            player.play();
        } catch (Exception e) {
            System.err.println("[SoundPlayer] failed to play " + effect + ": " + e.getMessage());
        }
    }

    private static synchronized void ensureJfx() {
        if (jfxPanel != null) {
            return;
        }
        Runnable init = () -> jfxPanel = new JFXPanel();
        if (SwingUtilities.isEventDispatchThread()) {
            init.run();
        } else {
            try {
                SwingUtilities.invokeAndWait(init);
            } catch (Exception e) {
                System.err.println("[SoundPlayer] failed to init JavaFX: " + e.getMessage());
            }
        }
    }
}
