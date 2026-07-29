package network;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class MatchmakingClient {
    private final String baseUrl;

    public MatchmakingClient(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    public boolean enqueue(String username, int elo, String serverId) {
        String body = encode("username", username)
                + "&" + encode("elo", String.valueOf(elo))
                + "&" + encode("serverId", serverId);
        return post("/enqueue", body) == 202;
    }

    public boolean cancel(String username) {
        String body = encode("username", username);
        return post("/cancel", body) == 200;
    }

    private int post(String path, String body) {
        try {
            URL url = new URL(baseUrl + path);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }
            return conn.getResponseCode();
        } catch (IOException e) {
            System.err.println("[MatchmakingClient] request failed: " + e.getMessage());
            return -1;
        }
    }

    private String encode(String key, String value) {
        try {
            return URLEncoder.encode(key, StandardCharsets.UTF_8.name())
                    + "="
                    + URLEncoder.encode(value, StandardCharsets.UTF_8.name());
        } catch (java.io.UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 is required", e);
        }
    }
}
