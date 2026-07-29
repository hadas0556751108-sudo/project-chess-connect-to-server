package server;

import auth.AuthManager;
import database.UserRepository;
import network.MatchmakingClient;
import redis.RedisClient;

public class ServerMain {
    public static void main(String[] args) throws Exception {
        String mongoUri = System.getenv().getOrDefault("MONGO_URI", "mongodb://localhost:27017/chess");
        int port = Integer.parseInt(System.getenv().getOrDefault("GAME_SERVER_PORT", "8080"));
        String gameServerId = System.getenv().getOrDefault("GAME_SERVER_ID", "game-server");
        String redisUrl = System.getenv().getOrDefault("REDIS_URL", "redis://localhost:6379");
        String matchmakingUrl = System.getenv().getOrDefault("MATCHMAKING_SERVICE_URL", "http://localhost:8082");

        UserRepository repository = new UserRepository(mongoUri);
        repository.init();

        AuthManager authManager = new AuthManager(repository);
        RedisClient redisClient = new RedisClient(redisUrl);
        MatchmakingClient matchmakingClient = new MatchmakingClient(matchmakingUrl);
        CommandRouter router = new CommandRouter(authManager, redisClient, matchmakingClient, gameServerId);
        ChessServer server = new ChessServer(port, router);
        server.start();

        System.out.println("Server running. Send SIGTERM to stop.");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                server.stop();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }));
        Thread.currentThread().join();
    }
}
