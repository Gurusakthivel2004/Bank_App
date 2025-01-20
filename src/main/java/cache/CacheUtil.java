package cache;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import redis.clients.jedis.Jedis;

public class CacheUtil {

	private static final Logger logger = LogManager.getLogger(CacheUtil.class);
	private final ObjectMapper objectMapper = new ObjectMapper();

	public <T> void save(String key, T value) {
		if (key == null || value == null) {
			logger.info("Key or value cannot be null.");
			return;
		}
		try (Jedis jedis = RedisCache.getConnection()) {
			String jsonValue = objectMapper.writeValueAsString(value);
			jedis.set(key, jsonValue);
			logger.info("Successfully saved key '{}' and value {} in Redis.", key, value);
		} catch (JsonProcessingException e) {
			logger.error("Failed to serialize value for key '{}': {}", key, e.getMessage());
		} catch (Exception e) {
			logger.error("Failed to save key '{}' in Redis: {}", key, e.getMessage());
		}
	}

	public <T> void saveWithTTL(String key, T value, int ttlSeconds) {
		if (key == null || value == null) {
			logger.error("Key or value cannot be null.");
			return;
		}
		try (Jedis jedis = RedisCache.getConnection()) {
			String jsonValue = objectMapper.writeValueAsString(value);
			jedis.setex(key, ttlSeconds, jsonValue);
			logger.info("Successfully saved key '{}' in Redis with TTL {} seconds.", key, ttlSeconds);
		} catch (JsonProcessingException e) {
			logger.error("Failed to serialize value for key '{}': {}", key, e.getMessage());
		} catch (Exception e) {
			logger.error("Failed to save key '{}' in Redis: {}", key, e.getMessage());
		}
	}

	public <T> T get(String key, TypeReference<T> typeReference) {
		if (key == null) {
			logger.info("Key cannot be null.");
			return null;
		}

		try (Jedis jedis = RedisCache.getConnection()) {
			String jsonValue = jedis.get(key);

			if (jsonValue != null) {
				T value = objectMapper.readValue(jsonValue, typeReference);
				logger.info("Successfully retrieved key '{}' from Redis.", key);
				return value;
			} else {
				logger.info("No value found for key '{}'.", key);
				return null;
			}
		} catch (Exception e) {
			logger.error("Failed to retrieve key '{}' from Redis: {}", key, e.getMessage());
			return null;
		}
	}

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
			jedis.set(key, jsonValue);
			logger.info("Successfully updated key '{}' in Redis.", key);
		} catch (JsonProcessingException e) {
			logger.error("Failed to serialize value for key '{}': {}", key, e.getMessage());
		} catch (Exception e) {
			logger.error("Failed to update key '{}' in Redis: {}", key, e.getMessage());
		}
	}

	public void delete(String deleteKey) {
		if (deleteKey == null) {
			logger.error("Key cannot be null.");
			return;
		}
		try (Jedis jedis = RedisCache.getConnection()) {
			List<String> allKeys = getAllKeys();
			List<String> matchingKeys = allKeys.stream().filter(key -> key.contains(deleteKey))
					.collect(Collectors.toList());
			for (String key : matchingKeys) {
				if (jedis.del(key) > 0) {
					logger.info("Successfully deleted key '{}' from Redis.", key);
				} else {
					logger.warn("Key '{}' does not exist in Redis.", key);
				}
			}
		} catch (Exception e) {
			logger.error("Failed to delete key '{}' from Redis: {}", deleteKey, e.getMessage());
		}
	}

	public void deleteAll() {
		try (Jedis jedis = RedisCache.getConnection()) {
			List<String> allKeys = getAllKeys();
			for (String key : allKeys) {
				if (jedis.del(key) > 0) {
					logger.info("Successfully deleted key '{}' from Redis.", key);
				} else {
					logger.warn("Key '{}' does not exist in Redis.", key);
				}
			}
		} catch (Exception e) {
			logger.error("Failed to delete from Redis: {}", e.getMessage());
		}
	}

	public List<String> getAllKeys() {
		try (Jedis jedis = RedisCache.getConnection()) {
			Set<String> keys = jedis.keys("*");
			return new ArrayList<>(keys);
		} catch (Exception e) {
			logger.error("Failed to retrieve keys from Redis: {}", e.getMessage());
			return Collections.emptyList();
		}
	}

	public <K, V> Map<K, V> getOrInitializeCache(String key, CacheUtil cacheService,
			TypeReference<Map<K, V>> typeReference) {
		Map<K, V> cachedData = get(key, typeReference);
		if (cachedData == null) {
			cachedData = new HashMap<>();
			cacheService.save(key, cachedData);
			logger.info("No existing cache found for key: {}. Initialized new map.", key);
		}
		return cachedData;
	}

	public <K, V> void saveCacheWithKey(Map<K, V> cache, K key, V value, String cacheKey) {
		cache.put(key, value);
		save(cacheKey, cache);
		logger.info("Updated cache with key: {} details", key);
	}

	public <V> List<V> getCachedList(String key, TypeReference<List<V>> typeReference) {
		List<V> cachedData = get(key, typeReference);

		if (cachedData != null) {
			if (cachedData.size() > 0) {
				logger.info("Details for key: {} found in cache.", cachedData);
				return cachedData;
			}
		}

		logger.info("No existing cache found for key: {}. Initialized new list.", key);
		return null;
	}

	public <V> List<V> getCachedList(String key, TypeReference<List<V>> typeReference, Map<String, Object> dataMap,
			String dataKey) {

		if (!dataMap.containsKey(dataKey)) {
			return null;
		}
		key += dataMap.get(dataKey);
		List<V> cachedData = get(key, typeReference);

		if (cachedData != null) {
			if (cachedData.size() > 0) {
				logger.info("Data for key: {} found in cache.", key);
				return cachedData;
			}
		}

		logger.info("No existing cache found for key: {}.", key);
		return null;
	}

}