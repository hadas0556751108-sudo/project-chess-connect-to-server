package network;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MatchmakingClientTest {

    private HttpServer server;
    private String lastBody;
    private final Map<String, Integer> overrideCodes = new HashMap<>();
    private MatchmakingClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/enqueue", new TestHandler(202, "ENQUEUED"));
        server.createContext("/cancel", new TestHandler(200, "CANCELLED"));
        server.start();

        int port = server.getAddress().getPort();
        client = new MatchmakingClient("http://localhost:" + port);
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
        overrideCodes.clear();
        lastBody = null;
    }

    @Test
    void enqueueSendsFormData() {
        assertTrue(client.enqueue("alice", 1500, "game-server"));
        assertNotNull(lastBody);
        assertTrue(lastBody.contains("username=alice"));
        assertTrue(lastBody.contains("elo=1500"));
        assertTrue(lastBody.contains("serverId=game-server"));
    }

    @Test
    void cancelSendsUsername() {
        assertTrue(client.cancel("alice"));
        assertNotNull(lastBody);
        assertTrue(lastBody.contains("username=alice"));
    }

    @Test
    void enqueueFailureReturnsFalse() {
        overrideCodes.put("/enqueue", 500);
        assertFalse(client.enqueue("alice", 1500, "game-server"));
    }

    @Test
    void cancelFailureReturnsFalse() {
        overrideCodes.put("/cancel", 500);
        assertFalse(client.cancel("alice"));
    }

    private class TestHandler implements HttpHandler {
        private final int defaultCode;
        private final String responseBody;

        TestHandler(int defaultCode, String responseBody) {
            this.defaultCode = defaultCode;
            this.responseBody = responseBody;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try (InputStream is = exchange.getRequestBody()) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buf = new byte[1024];
                int n;
                while ((n = is.read(buf)) != -1) {
                    out.write(buf, 0, n);
                }
                lastBody = new String(out.toByteArray(), StandardCharsets.UTF_8);
            }
            int code = overrideCodes.getOrDefault(exchange.getRequestURI().getPath(), defaultCode);
            byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(code, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }
}
