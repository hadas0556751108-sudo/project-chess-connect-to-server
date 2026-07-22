package server;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class DisconnectionManager {
    private static final int COUNTDOWN_SECONDS = 20;

    private final CommandRouter router;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
    private final Map<String, DisconnectTask> tasks = new ConcurrentHashMap<>();

    public DisconnectionManager(CommandRouter router) {
        this.router = router;
    }

    public void startCountdown(int roomId, String username, String color) {
        cancel(username);
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(new Runnable() {
            private int remaining = COUNTDOWN_SECONDS;

            @Override
            public void run() {
                if (remaining <= 0) {
                    router.endGameByDisconnect(roomId, username, color);
                    stopTask(username);
                    return;
                }
                router.broadcastDisconnectCountdown(roomId, remaining, username);
                remaining--;
            }
        }, 0, 1, TimeUnit.SECONDS);
        tasks.put(username, new DisconnectTask(roomId, future));
    }

    public void cancel(String username) {
        DisconnectTask task = tasks.remove(username);
        if (task != null) {
            task.future.cancel(false);
        }
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }

    private void stopTask(String username) {
        DisconnectTask task = tasks.remove(username);
        if (task != null) {
            task.future.cancel(false);
        }
    }

    private static class DisconnectTask {
        final int roomId;
        final ScheduledFuture<?> future;

        DisconnectTask(int roomId, ScheduledFuture<?> future) {
            this.roomId = roomId;
            this.future = future;
        }
    }
}
