package util;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import cache.RedisCache;
import enums.Constants.ModuleCode;
import enums.Constants.SymbolProvider;
import redis.clients.jedis.Jedis;

public class CRMQueueManager {

	private static Logger logger = LogManager.getLogger(CRMQueueManager.class);
	private static RedisCache redisCache = RedisCache.getInstance();

	public static <K extends SymbolProvider> void addUpdateJsonToSortedSet(String criteriaKey, Object criteriaValue,
			Map<K, Object> updateFields, String moduleName) throws Exception {

		ModuleCode moduleCode = ModuleCode.valueOf(moduleName.toUpperCase());
		if (moduleCode == null) {
			throw new IllegalArgumentException("Invalid module name: " + moduleName);
		}

		Map<String, Object> flatFields = new HashMap<>();
		for (Map.Entry<K, Object> entry : updateFields.entrySet()) {
			flatFields.put(entry.getKey().getSymbol(), entry.getValue());
		}

		Map<String, Object> payload = new HashMap<>();
		payload.put("Module_Code", moduleCode.getId());
		payload.put("Criteria_Key", criteriaKey);
		payload.put("Criteria_Value", criteriaValue);
		payload.put("Update_Fields", flatFields);

		CRMQueueManager.addToUpdateSet(payload);
	}

	public static void addToUpdateSet(Map<String, Object> payload) {
		try (Jedis jedis = redisCache.getConnection()) {
			String json = new ObjectMapper().writeValueAsString(payload);
			jedis.zadd("updateSet", System.currentTimeMillis(), json);
			logger.info("Successfully added payload to Redis updateSet: {}", json);
		} catch (JsonProcessingException e) {
			logger.error("Failed to serialize payload to JSON: {}", e.getMessage(), e);
		} catch (Exception e) {
			logger.error("Failed to add payload to Redis updateSet: {}", e.getMessage(), e);
		}
	}

}
