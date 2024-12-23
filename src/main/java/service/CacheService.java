package service;

import java.io.IOException;
import java.util.Map;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import redis.clients.jedis.Jedis;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import util.RedisCache;

public class CacheService {

	private static final Logger logger = LogManager.getLogger(CacheService.class);
	private final ObjectMapper objectMapper = new ObjectMapper();

	/**
	 * Save a key-value pair to Redis.
	 *
	 * @param key   the Redis key
	 * @param value the value to store
	 */
	public <T> void save(String key, T value) {
		if (key == null || value == null) {
			logger.error("Key or value cannot be null.");
			return;
		}
		try (Jedis jedis = RedisCache.getConnection()) {
			String jsonValue = objectMapper.writeValueAsString(value);
			jedis.set(key, jsonValue);
			logger.info("Successfully saved key '{}' in Redis.", key);
		} catch (JsonProcessingException e) {
			logger.error("Failed to serialize value for key '{}': {}", key, e.getMessage());
		} catch (Exception e) {
			logger.error("Failed to save key '{}' in Redis: {}", key, e.getMessage());
		}
	}
	
	/**
	 * Retrieves a Map from Redis where the key is of type K and the value is of type V.
	 * The method deserializes the JSON stored in Redis into a Map<K, V>. 
	 * 
	 * @param key       the Redis key for the Map
	 * @param keyClass  the class type of the key in the Map
	 * @param valueClass the class type of the value in the Map
	 * @return the deserialized Map<K, V> from Redis, or null if the key does not exist or deserialization fails
	 */
	public <K, V> Map<K, V> get(String key, Class<K> keyClass, Class<V> valueClass) {
		if (key == null || keyClass == null || valueClass == null) {
			logger.error("Key or class types cannot be null.");
			return null;
		}
		try (Jedis jedis = RedisCache.getConnection()) {
			if (!jedis.exists(key)) {
				logger.warn("Key '{}' does not exist in Redis.", key);
				return null;
			}

			String jsonValue = jedis.get(key);
			TypeReference<Map<K, V>> typeRef = new TypeReference<Map<K, V>>() {
			};
			Map<K, V> mapValue = objectMapper.readValue(jsonValue, typeRef);

			logger.info("Successfully retrieved key '{}' from Redis.", key);
			return mapValue;
		} catch (IOException e) {
			logger.error("Failed to deserialize value for key '{}': {}", key, e.getMessage());
		} catch (Exception e) {
			logger.error("Failed to retrieve key '{}' from Redis: {}", key, e.getMessage());
		}
		return null;
	}

	/**
	 * Update the value of an existing key in Redis.
	 *
	 * @param key   the Redis key
	 * @param value the new value to store
	 */
	public <T> void update(String key, T value) {
		if (key == null || value == null) {
			logger.error("Key or value cannot be null.");
			return;
		}
		try (Jedis jedis = RedisCache.getConnection()) {
			if (!jedis.exists(key)) {
				logger.warn("Key '{}' does not exist in Redis. Inserting new value.", key);
			}
			String jsonValue = objectMapper.writeValueAsString(value);
			jedis.set(key, jsonValue); // This will overwrite the existing value
			logger.info("Successfully updated key '{}' in Redis.", key);
		} catch (JsonProcessingException e) {
			logger.error("Failed to serialize value for key '{}': {}", key, e.getMessage());
		} catch (Exception e) {
			logger.error("Failed to update key '{}' in Redis: {}", key, e.getMessage());
		}
	}

	/**
	 * Delete a key from Redis.
	 *
	 * @param key the Redis key
	 */
	public void delete(String key) {
		if (key == null) {
			logger.error("Key cannot be null.");
			return;
		}
		try (Jedis jedis = RedisCache.getConnection()) {
			if (jedis.del(key) > 0) {
				logger.info("Successfully deleted key '{}' from Redis.", key);
			} else {
				logger.warn("Key '{}' does not exist in Redis.", key);
			}
		} catch (Exception e) {
			logger.error("Failed to delete key '{}' from Redis: {}", key, e.getMessage());
		}
	}
}