package matchmaking;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import redis.RedisClient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class MatchmakingServiceMain {
    private static final String QUEUE_KEY = "matchmaking:queue";
    private static final String PLAYER_PREFIX = "matchmaking:player:";

    public static void main(String[] args) throws Exception {
        String redisUrl = System.getenv().getOrDefault("REDIS_URL", "redis://redis:6379");
        int port = Integer.parseInt(System.getenv().getOrDefault("MATCHMAKING_PORT", "8082"));
        int eloRange = Integer.parseInt(System.getenv().getOrDefault("ELO_RANGE", "100"));
        int timeoutSeconds = Integer.parseInt(System.getenv().getOrDefault("MATCH_TIMEOUT_SECONDS", "60"));
        String matchmakingId = System.getenv().getOrDefault("MATCHMAKING_ID", "matchmaking-1");

        RedisClient redis = new RedisClient(redisUrl);
        MatchmakingScanner scanner = new MatchmakingScanner(redis, eloRange, timeoutSeconds * 1000L, matchmakingId);
        Thread scannerThread = new Thread(scanner);
        scannerThread.start();

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/enqueue", new EnqueueHandler(redis));
        server.createContext("/cancel", new CancelHandler(redis));
        server.createContext("/health", new HealthHandler());
        server.start();

        System.out.println("Matchmaking service started on port " + port);
    }

    static String getParam(String body, String name) {
        if (body == null) return null;
        String[] pairs = body.split("&");
        for (String pair : pairs) {
            int eq = pair.indexOf('=');
            if (eq > 0) {
                String key = pair.substring(0, eq);
                String value = pair.substring(eq + 1);
                if (key.equals(name)) {
                    try {
                        return java.net.URLDecoder.decode(value, StandardCharsets.UTF_8.name());
                    } catch (java.io.UnsupportedEncodingException e) {
                        throw new IllegalStateException("UTF-8 is required", e);
                    }
                }
            }
        }
        return null;
    }

    static void sendResponse(HttpExchange exchange, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    static class EnqueueHandler implements HttpHandler {
        private final RedisClient redis;

        EnqueueHandler(RedisClient redis) {
            this.redis = redis;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method not allowed");
                return;
            }
            String body;
            try (InputStream is = exchange.getRequestBody()) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buf = new byte[1024];
                int n;
                while ((n = is.read(buf)) != -1) {
                    out.write(buf, 0, n);
                }
                body = new String(out.toByteArray(), StandardCharsets.UTF_8);
            }
            String username = getParam(body, "username");
            String eloParam = getParam(body, "elo");
            String serverId = getParam(body, "serverId");

            if (username == null || eloParam == null || serverId == null) {
                sendResponse(exchange, 400, "Missing username, elo or serverId");
                return;
            }

            int elo;
            try {
                elo = Integer.parseInt(eloParam);
            } catch (NumberFormatException e) {
                sendResponse(exchange, 400, "Invalid elo");
                return;
            }

            redis.hset(PLAYER_PREFIX + username, "elo", String.valueOf(elo));
            redis.hset(PLAYER_PREFIX + username, "serverId", serverId);
            redis.hset(PLAYER_PREFIX + username, "enqueuedAt", String.valueOf(System.currentTimeMillis()));
            redis.zadd(QUEUE_KEY, elo, username);

            sendResponse(exchange, 202, "ENQUEUED " + username);
        }
    }

    static class CancelHandler implements HttpHandler {
        private final RedisClient redis;

        CancelHandler(RedisClient redis) {
            this.redis = redis;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method not allowed");
                return;
            }
            String body;
            try (InputStream is = exchange.getRequestBody()) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buf = new byte[1024];
                int n;
                while ((n = is.read(buf)) != -1) {
                    out.write(buf, 0, n);
                }
                body = new String(out.toByteArray(), StandardCharsets.UTF_8);
            }
            String username = getParam(body, "username");
            if (username == null) {
                sendResponse(exchange, 400, "Missing username");
                return;
            }

            redis.zrem(QUEUE_KEY, username);
            redis.hdel(PLAYER_PREFIX + username, "elo", "serverId", "enqueuedAt");

            sendResponse(exchange, 200, "CANCELLED " + username);
        }
    }

    static class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            sendResponse(exchange, 200, "OK");
        }
    }
}
