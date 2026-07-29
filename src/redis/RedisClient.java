package redis;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;
import redis.clients.jedis.resps.Tuple;

import java.util.List;

public class RedisClient implements AutoCloseable {
    private final JedisPool pool;

    public RedisClient(String redisUrl) {
        this.pool = createPool(redisUrl);
    }

    private static JedisPool createPool(String redisUrl) {
        if (redisUrl.startsWith("redis://")) {
            String hostPort = redisUrl.substring(8);
            int colon = hostPort.lastIndexOf(':');
            if (colon > 0) {
                String host = hostPort.substring(0, colon);
                int port = Integer.parseInt(hostPort.substring(colon + 1));
                return new JedisPool(host, port);
            }
            return new JedisPool(hostPort);
        }
        return new JedisPool(redisUrl);
    }

    public Jedis getResource() {
        return pool.getResource();
    }

    public List<Tuple> zrangeByScoreWithScores(String key, double min, double max) {
        try (Jedis jedis = pool.getResource()) {
            return jedis.zrangeByScoreWithScores(key, min, max);
        }
    }

    public void zadd(String key, double score, String member) {
        try (Jedis jedis = pool.getResource()) {
            jedis.zadd(key, score, member);
        }
    }

    public void zrem(String key, String... members) {
        try (Jedis jedis = pool.getResource()) {
            jedis.zrem(key, members);
        }
    }

    public void hset(String key, String field, String value) {
        try (Jedis jedis = pool.getResource()) {
            jedis.hset(key, field, value);
        }
    }

    public String hget(String key, String field) {
        try (Jedis jedis = pool.getResource()) {
            return jedis.hget(key, field);
        }
    }

    public void hdel(String key, String... fields) {
        try (Jedis jedis = pool.getResource()) {
            jedis.hdel(key, fields);
        }
    }

    public void publish(String channel, String message) {
        try (Jedis jedis = pool.getResource()) {
            jedis.publish(channel, message);
        }
    }

    public void subscribe(String[] channels, JedisPubSub listener) {
        try (Jedis jedis = pool.getResource()) {
            jedis.subscribe(listener, channels);
        }
    }

    @Override
    public void close() {
        pool.close();
    }
}
