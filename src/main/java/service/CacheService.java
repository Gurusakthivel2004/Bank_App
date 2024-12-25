package service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

	public <K, V> Map<K, V> get(String key, TypeReference<Map<K, V>> typeRef) {
		if (key == null || typeRef == null) {
			logger.error("Key or type reference cannot be null.");
			return null;
		}
		try (Jedis jedis = RedisCache.getConnection()) {
			if (!jedis.exists(key)) {
				logger.warn("Key '{}' does not exist in Redis.", key);
				return null;
			}

			String jsonValue = jedis.get(key);
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
			jedis.set(key, jsonValue); // This will overwrite the existing value
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

	public List<String> getAllKeys() {
		try (Jedis jedis = RedisCache.getConnection()) {
			Set<String> keys = jedis.keys("*"); 
			return new ArrayList<>(keys);
		} catch (Exception e) {
			logger.error("Failed to retrieve keys from Redis: {}", e.getMessage());
			return Collections.emptyList();
		}
	}

}