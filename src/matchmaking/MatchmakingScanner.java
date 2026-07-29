package matchmaking;

import redis.RedisClient;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.resps.Tuple;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MatchmakingScanner implements Runnable {
    private static final String QUEUE_KEY = "matchmaking:queue";
    private static final String PLAYER_PREFIX = "matchmaking:player:";
    private static final String MATCH_FOUND_CHANNEL_PREFIX = "match:found:";
    private static final String MATCH_TIMEOUT_CHANNEL_PREFIX = "match:timeout:";

    private final RedisClient redis;
    private final int baseEloRange;
    private final long matchTimeoutMs;
    private final String matchmakingId;
    private volatile boolean running = true;

    public MatchmakingScanner(RedisClient redis, int baseEloRange, long matchTimeoutMs, String matchmakingId) {
        this.redis = redis;
        this.baseEloRange = baseEloRange;
        this.matchTimeoutMs = matchTimeoutMs;
        this.matchmakingId = matchmakingId;
    }

    public void shutdown() {
        running = false;
    }

    @Override
    public void run() {
        while (running) {
            try {
                processTimeouts();
                findMatches();
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("[matchmaking:" + matchmakingId + "] scanner error: " + e.getMessage());
            }
        }
    }

    private void processTimeouts() {
        List<Tuple> all = redis.zrangeByScoreWithScores(QUEUE_KEY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        long now = System.currentTimeMillis();
        for (Tuple tuple : all) {
            String username = tuple.getElement();
            String enqueuedAt = redis.hget(PLAYER_PREFIX + username, "enqueuedAt");
            if (enqueuedAt == null) {
                redis.zrem(QUEUE_KEY, username);
                continue;
            }
            long elapsed = now - Long.parseLong(enqueuedAt);
            if (elapsed >= matchTimeoutMs) {
                String serverId = redis.hget(PLAYER_PREFIX + username, "serverId");
                removePlayer(username);
                if (serverId != null) {
                    redis.publish(MATCH_TIMEOUT_CHANNEL_PREFIX + serverId, username);
                }
            }
        }
    }

    private void findMatches() {
        List<Tuple> all = redis.zrangeByScoreWithScores(QUEUE_KEY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
        if (all.size() < 2) {
            return;
        }

        List<String> matched = new ArrayList<>();
        for (Tuple tuple : all) {
            String p1 = tuple.getElement();
            if (matched.contains(p1)) {
                continue;
            }

            PlayerInfo i1 = loadPlayer(p1);
            if (i1 == null) {
                redis.zrem(QUEUE_KEY, p1);
                continue;
            }

            long elapsed = System.currentTimeMillis() - i1.enqueuedAt;
            int range = baseEloRange + (int) (elapsed / 2000);

            List<Tuple> candidates = redis.zrangeByScoreWithScores(QUEUE_KEY, i1.elo - range, i1.elo + range);
            for (Tuple candidate : candidates) {
                String p2 = candidate.getElement();
                if (p2.equals(p1) || matched.contains(p2)) {
                    continue;
                }

                PlayerInfo i2 = loadPlayer(p2);
                if (i2 == null || !i2.serverId.equals(i1.serverId)) {
                    continue;
                }

                if (removePair(p1, p2)) {
                    matched.add(p1);
                    matched.add(p2);
                    redis.publish(MATCH_FOUND_CHANNEL_PREFIX + i1.serverId, p1 + " " + p2);
                    break;
                }
            }
        }
    }

    private PlayerInfo loadPlayer(String username) {
        String elo = redis.hget(PLAYER_PREFIX + username, "elo");
        String serverId = redis.hget(PLAYER_PREFIX + username, "serverId");
        String enqueuedAt = redis.hget(PLAYER_PREFIX + username, "enqueuedAt");
        if (elo == null || serverId == null || enqueuedAt == null) {
            return null;
        }
        return new PlayerInfo(username, Integer.parseInt(elo), serverId, Long.parseLong(enqueuedAt));
    }

    private boolean removePair(String p1, String p2) {
        String script = "local removed = redis.call('ZREM', KEYS[1], ARGV[1], ARGV[2]) " +
                "if removed == 2 then " +
                "  redis.call('DEL', KEYS[2], KEYS[3]) " +
                "  return 1 " +
                "end " +
                "return 0";

        try (Jedis jedis = redis.getResource()) {
            Object result = jedis.eval(script, 3, QUEUE_KEY, PLAYER_PREFIX + p1, PLAYER_PREFIX + p2, p1, p2);
            return result instanceof Number && ((Number) result).longValue() == 1L;
        }
    }

    private void removePlayer(String username) {
        try (Jedis jedis = redis.getResource()) {
            jedis.zrem(QUEUE_KEY, username);
            jedis.del(PLAYER_PREFIX + username);
        }
    }

    private static class PlayerInfo {
        final String username;
        final int elo;
        final String serverId;
        final long enqueuedAt;

        PlayerInfo(String username, int elo, String serverId, long enqueuedAt) {
            this.username = username;
            this.elo = elo;
            this.serverId = serverId;
            this.enqueuedAt = enqueuedAt;
        }
    }
}
