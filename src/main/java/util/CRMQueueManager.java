package util;

<<<<<<< HEAD
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
=======
import java.util.HashMap;
>>>>>>> 7e942af (CRMSchedular update)
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
<<<<<<< HEAD
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import cache.RedisCache;
=======
import com.fasterxml.jackson.databind.ObjectMapper;

import cache.RedisCache;
import enums.Constants.ModuleCode;
>>>>>>> 7e942af (CRMSchedular update)
import enums.Constants.SymbolProvider;
import redis.clients.jedis.Jedis;

public class CRMQueueManager {

	private static Logger logger = LogManager.getLogger(CRMQueueManager.class);
	private static RedisCache redisCache = RedisCache.getInstance();

<<<<<<< HEAD
	public static String prepareUpdatePayloadFromRedis() {
		List<Map<String, Object>> records = new ArrayList<>();

		try (Jedis jedis = redisCache.getConnection()) {

			List<String> payloads = jedis.zrange("updateSet", 0, 99);

			for (String payload : payloads) {
				Map<String, Object> record = new ObjectMapper().readValue(payload,
						new TypeReference<Map<String, Object>>() {
						});
				records.add(record);
			}

			Map<String, Object> finalPayload = new HashMap<>();
			finalPayload.put("data", records);

			return new ObjectMapper().writeValueAsString(finalPayload);

		} catch (Exception e) {
			logger.error("Error preparing update payload from Redis: {}", e.getMessage(), e);
		}
		return null;
=======
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
>>>>>>> 7e942af (CRMSchedular update)
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
