package util;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RedisCache {
	
	private static JedisPool jedisPool;
	private static final Logger logger = LogManager.getLogger(RedisCache.class);

	static {
		try {
			logger.info("creating jedis connection...");
			jedisPool = new JedisPool("localhost", 6379); 
		} catch (Exception e) {
			logger.error("Failed to initialize JedisPool");
			throw new ExceptionInInitializerError("Failed to initialize JedisPool: " + e.getMessage());
		}
	}

	public static Jedis getConnection() {
		return jedisPool.getResource();
	}

	public static void closeConnection(Jedis jedis) {
		if (jedis != null) {
			jedis.close();
		}
	}
}