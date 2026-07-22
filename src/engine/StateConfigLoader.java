package engine;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class StateConfigLoader {
    private final String resourcesPath;
    private final double pixelsPerMeter;
    private final Map<String, StateConfig> configs = new HashMap<>();

    public StateConfigLoader(String resourcesPath, double pixelsPerMeter) {
        this.resourcesPath = resourcesPath;
        this.pixelsPerMeter = pixelsPerMeter;
        loadAllConfigs();
    }

    private void loadAllConfigs() {
        String[] pieces = {"wP", "wR", "wN", "wB", "wQ", "wK", "bP", "bR", "bN", "bB", "bQ", "bK"};
        String[] states = {"idle", "move", "jump", "short_rest", "long_rest"};

        for (String piece : pieces) {
            for (String state : states) {
                String configPath = resourcesPath + "/" + piece + "/states/" + state + "/config.json";
                try {
                    int frameCount = countFrames(piece, state);
                    StateConfig config = loadConfig(configPath, frameCount);
                    String key = piece + "_" + state;
                    configs.put(key, config);
                } catch (IOException e) {
                    System.err.println("Could not load config: " + configPath);
                }
            }
        }
    }

    private StateConfig loadConfig(String path, int frameCount) throws IOException {
        // Simple JSON parsing for our specific format
        FileReader reader = new FileReader(new File(path));
        StringBuilder content = new StringBuilder();
        char[] buffer = new char[1024];
        int read;
        while ((read = reader.read(buffer)) != -1) {
            content.append(buffer, 0, read);
        }
        reader.close();

        String json = content.toString();

        // Parse speed_m_per_sec
        double speed = extractDouble(json, "speed_m_per_sec");
        // Parse next_state_when_finished
        String nextState = extractString(json, "next_state_when_finished");
        // Parse frames_per_sec
        int framesPerSec = extractInt(json, "frames_per_sec");
        // Parse is_loop
        boolean isLoop = extractBoolean(json, "is_loop");

        return new StateConfig(speed, nextState, framesPerSec, isLoop, frameCount);
    }

    private int countFrames(String piece, String state) {
        String spritesPath = resourcesPath + "/" + piece + "/states/" + state + "/sprites";
        File spritesDir = new File(spritesPath);
        if (!spritesDir.exists() || !spritesDir.isDirectory()) {
            return 0;
        }
        int count = 0;
        for (File file : spritesDir.listFiles()) {
            if (file.isFile() && file.getName().endsWith(".png")) {
                count++;
            }
        }
        return count;
    }

    private double extractDouble(String json, String key) {
        String search = "\"" + key + "\":";
        int start = json.indexOf(search);
        if (start == -1) return 0.0;
        start += search.length();
        int end = json.indexOf(",", start);
        if (end == -1) end = json.indexOf("}", start);
        String value = json.substring(start, end).trim();
        return Double.parseDouble(value);
    }

    private String extractString(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start == -1) return "";
        start += search.length();
        int end = json.indexOf("\"", start);
        return json.substring(start, end);
    }

    private int extractInt(String json, String key) {
        String search = "\"" + key + "\":";
        int start = json.indexOf(search);
        if (start == -1) return 0;
        start += search.length();
        int end = json.indexOf(",", start);
        if (end == -1) end = json.indexOf("}", start);
        String value = json.substring(start, end).trim();
        return Integer.parseInt(value);
    }

    private boolean extractBoolean(String json, String key) {
        String search = "\"" + key + "\":";
        int start = json.indexOf(search);
        if (start == -1) return false;
        start += search.length();
        int end = json.indexOf(",", start);
        if (end == -1) end = json.indexOf("}", start);
        String value = json.substring(start, end).trim();
        return Boolean.parseBoolean(value);
    }

    public StateConfig getConfig(String pieceKey, String stateName) {
        return configs.get(pieceKey + "_" + stateName);
    }

    public double getSpeedPixelsPerSec(String pieceKey, String stateName) {
        StateConfig config = getConfig(pieceKey, stateName);
        if (config == null) return 0.0;
        return config.speedMetersPerSec() * pixelsPerMeter;
    }
}
