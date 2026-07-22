package server;

import network.Protocol;
import org.java_websocket.WebSocket;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class MatchmakingManager {
    private static final int ELO_RANGE = 100;
    private static final long MATCH_TIMEOUT_MS = 60000;

    private final CommandRouter router;
    private final List<QueuedPlayer> queue = new CopyOnWriteArrayList<>();
    private final Map<String, ScheduledFuture<?>> timeoutTasks = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private volatile boolean running = true;

    public MatchmakingManager(CommandRouter router) {
        this.router = router;
        scheduler.scheduleAtFixedRate(this::scanQueue, 1, 1, TimeUnit.SECONDS);
    }

    public void enqueue(WebSocket connection, String username, int elo) {
        remove(username);
        queue.add(new QueuedPlayer(connection, username, elo, System.currentTimeMillis()));
        ScheduledFuture<?> future = scheduler.schedule(() -> {
            remove(username);
            if (connection != null && connection.isOpen()) {
                connection.send(Protocol.NO_MATCH);
            }
        }, MATCH_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        timeoutTasks.put(username, future);
    }

    public void cancel(String username) {
        remove(username);
    }

    public void shutdown() {
        running = false;
        scheduler.shutdownNow();
    }

    private void remove(String username) {
        queue.removeIf(p -> p.username.equals(username));
        ScheduledFuture<?> future = timeoutTasks.remove(username);
        if (future != null) {
            future.cancel(false);
        }
    }

    private void scanQueue() {
        if (!running) {
            return;
        }
        List<QueuedPlayer> matched = new ArrayList<>();
        for (QueuedPlayer p1 : queue) {
            if (matched.contains(p1)) {
                continue;
            }
            for (QueuedPlayer p2 : queue) {
                if (p1 == p2 || matched.contains(p2)) {
                    continue;
                }
                if (Math.abs(p1.elo - p2.elo) <= ELO_RANGE) {
                    matched.add(p1);
                    matched.add(p2);
                    remove(p1.username);
                    remove(p2.username);
                    router.startMatch(p1.username, p2.username);
                    break;
                }
            }
        }
    }

    private static class QueuedPlayer {
        final WebSocket connection;
        final String username;
        final int elo;
        final long startTime;

        QueuedPlayer(WebSocket connection, String username, int elo, long startTime) {
            this.connection = connection;
            this.username = username;
            this.elo = elo;
            this.startTime = startTime;
        }
    }
}
