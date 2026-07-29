package auth;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import database.UserRepository;
import model.User;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

public class AuthServiceMain {
    public static void main(String[] args) throws Exception {
        String mongoUri = System.getenv().getOrDefault("MONGO_URI", "mongodb://mongodb:27017/chess");
        String jwtSecret = System.getenv().getOrDefault("JWT_SECRET", "dev-secret-do-not-use-in-production");
        int port = Integer.parseInt(System.getenv().getOrDefault("AUTH_PORT", "8081"));

        UserRepository repository = new UserRepository(mongoUri);
        repository.init();
        AuthManager authManager = new AuthManager(repository, jwtSecret);

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/register", new RegisterHandler(authManager));
        server.createContext("/login", new LoginHandler(authManager));
        server.start();

        System.out.println("Auth service started on port " + port);
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

    static class RegisterHandler implements HttpHandler {
        private final AuthManager authManager;

        RegisterHandler(AuthManager authManager) {
            this.authManager = authManager;
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
            String password = getParam(body, "password");
            if (username == null || password == null) {
                sendResponse(exchange, 400, "Missing username or password");
                return;
            }
            if (authManager.register(username, password)) {
                sendResponse(exchange, 201, "REGISTER_SUCCESS " + username);
            } else {
                sendResponse(exchange, 409, "REGISTER_FAILED username taken or invalid");
            }
        }
    }

    static class LoginHandler implements HttpHandler {
        private final AuthManager authManager;

        LoginHandler(AuthManager authManager) {
            this.authManager = authManager;
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
            String password = getParam(body, "password");
            if (username == null || password == null) {
                sendResponse(exchange, 400, "Missing username or password");
                return;
            }
            User user = authManager.authenticate(username, password);
            if (user == null) {
                sendResponse(exchange, 401, "LOGIN_FAILED invalid username or password");
                return;
            }
            String token = authManager.generateToken(user);
            sendResponse(exchange, 200, "LOGIN_SUCCESS " + username + " " + user.getElo() + " " + token);
        }
    }
}
