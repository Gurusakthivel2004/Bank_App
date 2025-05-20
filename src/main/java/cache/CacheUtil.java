package cache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import redis.clients.jedis.Jedis;

public class CacheUtil {

	private static Logger logger = LogManager.getLogger(CacheUtil.class);
	private static ObjectMapper objectMapper = new ObjectMapper();
	private static RedisCache redisCache = RedisCache.getInstance();

	public static <T> void save(String key, T value) {
		if (key == null || value == null) {
			logger.info("Key or value cannot be null.");
			return;
		}
		try (Jedis jedis = redisCache.getConnection()) {
			String jsonValue = objectMapper.writeValueAsString(value);
			jedis.set(key, jsonValue);
			logger.info("Successfully saved key '{}' and value {} in Redis.", key, value);
		} catch (JsonProcessingException e) {
			logger.error("Failed to serialize value for key '{}': {}", key, e.getMessage());
		} catch (Exception e) {
			logger.error("Failed to save key '{}' in Redis: {}", key, e.getMessage());
		}
	}

	public static <T> void saveWithTTL(String key, T value, int ttlSeconds) {
		if (key == null || value == null) {
			logger.error("Key or value cannot be null.");
			return;
		}
		try (Jedis jedis = redisCache.getConnection()) {
			String jsonValue = objectMapper.writeValueAsString(value);
			jedis.setex(key, ttlSeconds, jsonValue);
			logger.info("Successfully saved key '{}' in Redis with TTL {} seconds.", key, ttlSeconds);
		} catch (JsonProcessingException e) {
			logger.error("Failed to serialize value for key '{}': {}", key, e.getMessage());
		} catch (Exception e) {
			logger.error("Failed to save key '{}' in Redis: {}", key, e.getMessage());
		}
	}

	public static <T> T get(String key, TypeReference<T> typeReference) {
		if (key == null) {
			logger.info("Key cannot be null.");
			return null;
		}

		try (Jedis jedis = redisCache.getConnection()) {
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

	public static void delete(String deleteKey) {
		if (deleteKey == null) {
			logger.error("Key cannot be null.");
			return;
		}
		try (Jedis jedis = redisCache.getConnection()) {
			if (jedis.del(deleteKey) > 0) {
				logger.info("Successfully deleted key '{}' from Redis.", deleteKey);
			}
		} catch (Exception e) {
			logger.error("Failed to delete key '{}' from Redis: {}", deleteKey, e.getMessage());
		}
	}

	public static void deleteAll() {
		try (Jedis jedis = redisCache.getConnection()) {
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

	public static String getCRMRecordId(String moduleName, Object primaryKey) {
	    if (moduleName == null || primaryKey == null) {
	        logger.error("ModuleName or PrimaryKey cannot be null.");
	        return null;
	    }
	    try {
	        Map<String, String> moduleCache = get(moduleName, new TypeReference<Map<String, String>>() {});
	        if (moduleCache != null) {
	            return moduleCache.get(primaryKey);
	        }
	    } catch (Exception e) {
	        logger.error("Failed to get CRM recordId for module '{}' and key '{}': {}", moduleName, primaryKey, e.getMessage());
	    }
	    return null;
	}

	public static void saveCRMRecordId(String moduleName, Object primaryKey, String recordId) {
	    if (moduleName == null || primaryKey == null || recordId == null) {
	        logger.error("ModuleName, PrimaryKey, or RecordId cannot be null.");
	        return;
	    }
	    try {
	        Map<Object, Object> moduleCache = get(moduleName, new TypeReference<Map<Object, Object>>() {});
	        if (moduleCache == null) {
	            moduleCache = new HashMap<>();
	        }
	        moduleCache.put(primaryKey, recordId);
	        save(moduleName, moduleCache);
	        logger.info("Successfully saved CRM recordId '{}' under module '{}' for key '{}'.", recordId, moduleName, primaryKey);
	    } catch (Exception e) {
	        logger.error("Failed to save CRM recordId for module '{}' and key '{}': {}", moduleName, primaryKey, e.getMessage());
	    }
	}

	public static List<String> getAllKeys() {
		try (Jedis jedis = redisCache.getConnection()) {
			Set<String> keys = jedis.keys("*");
			return new ArrayList<>(keys);
		} catch (Exception e) {
			logger.error("Failed to retrieve keys from Redis: {}", e.getMessage());
			return Collections.emptyList();
		}
	}

	public static <K, V> Map<K, V> getOrInitializeCache(String key, CacheUtil cacheService,
			TypeReference<Map<K, V>> typeReference) {
		Map<K, V> cachedData = get(key, typeReference);
		if (cachedData == null) {
			cachedData = new HashMap<>();
			save(key, cachedData);
			logger.info("No existing cache found for key: {}. Initialized new map.", key);
		}
		return cachedData;
	}

	public static <V> List<V> getCachedList(String key, TypeReference<List<V>> typeReference) {
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

	public static <V> List<V> getCachedList(String key, TypeReference<List<V>> typeReference,
			Map<String, Object> dataMap, String dataKey) {
		if (!dataMap.containsKey(dataKey)) {
			return null;
		}
		key += dataMap.get(dataKey);
		return getCachedList(key, typeReference);
	}

}