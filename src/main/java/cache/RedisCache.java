package cache;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import servlet.Initializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RedisCache {

	private static Logger logger = LogManager.getLogger(RedisCache.class);
	private JedisPool jedisPool;

	private RedisCache() {
		this.jedisPool = Initializer.getJedisPool();
	}

	private static class Holder {
		private static final RedisCache INSTANCE = new RedisCache();
	}

	public static RedisCache getInstance() {
		return Holder.INSTANCE;
	}

	public Jedis getConnection() {
		try {
			return jedisPool.getResource();
		} catch (Exception e) {
			logger.error("Failed to get Redis connection: {}", e.getMessage());
			return null;
		}
	}

	public void closeConnection(Jedis jedis) {
		if (jedis != null) {
			jedis.close();
		}
	}

}