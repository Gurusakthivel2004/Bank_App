package schedular;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import cache.CacheUtil;
import cache.RedisCache;
import crm.BulkWriteService;
import crm.CRMHttpService;
import enums.Constants.ModuleCode;
import redis.clients.jedis.Jedis;
import util.JsonUtils;
import util.OAuthConfig;

public class CRMSchedular {

	private static final int RECORD_THRESHOLD = 5;
	private static final Logger LOGGER = LogManager.getLogger(CRMSchedular.class);

	private static final ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(1);

	private static final RedisCache REDIS_CACHE = RedisCache.getInstance();
	private static final CRMHttpService CRM_HTTP_SERVICE = CRMHttpService.getInstance();

	public void startScheduler() {
		ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();

		Runnable task = () -> {
			Thread.currentThread().setContextClassLoader(contextClassLoader);

			LOGGER.info("CRM update check started...");

			try {
				processUpdateSet();
			} catch (Exception e) {
				LOGGER.error("Error during CRM update check: {}", e.getMessage(), e);
			}

			LOGGER.info("Password update check completed.");
		};

		SCHEDULER.scheduleAtFixedRate(task, 0, 30, TimeUnit.MINUTES);
		LOGGER.info("Password update scheduler started: runs every 24 hours.");
	}

	public void processUpdateSet() {
		try (Jedis jedis = REDIS_CACHE.getConnection()) {
			List<String> entries = fetchEntriesFromUpdateSet(jedis);
			if (entries.isEmpty())
				return;

			Map<String, List<Map<String, String>>> moduleData = prepareModuleData(entries);
			processModuleData(moduleData);

			jedis.zremrangeByRank("updateSet", 0, entries.size() - 1);
			LOGGER.info("Processed and removed {} records from updateSet.", entries.size());

		} catch (Exception e) {
			LOGGER.error("Error processing updateSet: {}", e.getMessage(), e);
		}
	}

	private List<String> fetchEntriesFromUpdateSet(Jedis jedis) {
		long size = jedis.zcard("updateSet");

		if (size == 0) {
			LOGGER.info("No records to update in updateSet.");
			return Collections.emptyList();
		}

		LOGGER.info("Total records to process in updateSet: {}", size);

		List<String> entries = jedis.zrange("updateSet", 0, Math.min(size, 100) - 1);
		LOGGER.info("Fetched {} entries from updateSet for processing.", entries.size());

		return entries;
	}

	private Map<String, List<Map<String, String>>> prepareModuleData(List<String> entries) throws Exception {
		Map<String, List<Map<String, String>>> moduleData = new HashMap<>();
		ObjectMapper mapper = new ObjectMapper();

		for (String entry : entries) {
			Map<String, String> record = mapper.readValue(entry, new TypeReference<Map<String, String>>() {
			});
			Integer moduleCodeId = Integer.parseInt(record.get("Module_Code"));
			ModuleCode moduleCode = ModuleCode.fromId(moduleCodeId);
			String criteriaKey = record.get("Criteria_Key");
			String criteriaValue = record.get("Criteria_Value");
			String module = moduleCode.name();

			LOGGER.debug("Processing record for module: {}, criteria: {}={}", module, criteriaKey, criteriaValue);

			String recordId = resolveRecordId(moduleCode, criteriaKey, criteriaValue);
			Map<String, String> updateFields = mapper.readValue(record.get("Update_Fields"),
					new TypeReference<Map<String, String>>() {
					});
			updateFields.put("id", recordId);

			if (updateFields.containsKey("Phone")) {
				LOGGER.debug("Phone number present in updateFields for module {}: {}", module,
						updateFields.get("Phone"));
				// Optional: Add validation
			}

			LOGGER.debug("Update fields prepared for record {}: {}", recordId, updateFields);
			moduleData.computeIfAbsent(module, k -> new ArrayList<>()).add(updateFields);
		}

		return moduleData;
	}

	private String resolveRecordId(ModuleCode moduleCode, String criteriaKey, String criteriaValue) throws Exception {
		String module = moduleCode.name();
		String recordId = CacheUtil.getCRMRecordId(module, criteriaValue);

		if (recordId != null) {
			LOGGER.info("Cache hit: Found recordId in cache for {}: {}", module, recordId);
			return recordId;
		}

		String endpoint = OAuthConfig.get("crm." + module.toLowerCase() + ".endpoint");
		String jsonResponse = CRM_HTTP_SERVICE.fetchRecord(criteriaKey, criteriaValue, endpoint);
		recordId = JsonUtils.getValueByPath(jsonResponse, "data[0]", "id");

		LOGGER.info("Fetched recordId from CRM for module {} and value {}: {}", module, criteriaValue, recordId);
		CacheUtil.saveCRMRecordId(module, criteriaValue, recordId);
		LOGGER.debug("Saved recordId to cache: {} -> {}", criteriaValue, recordId);

		return recordId;
	}

	private void processModuleData(Map<String, List<Map<String, String>>> moduleData) throws Exception {
		for (Map.Entry<String, List<Map<String, String>>> entry : moduleData.entrySet()) {
			processModuleUpdates(entry.getKey(), entry.getValue());
		}
	}

	private void processModuleUpdates(String module, List<Map<String, String>> updates) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		LOGGER.info("Total records for module {}: {}", module, updates.size());

		if (updates.size() <= RECORD_THRESHOLD) {
			Map<String, Object> payload = Collections.singletonMap("data", updates);
			String json = mapper.writeValueAsString(payload);

			LOGGER.info("Sending direct update to CRM for module {} with {} records", module, updates.size());
			LOGGER.debug("JSON Payload: {}", json);

			CRM_HTTP_SERVICE.putToCrm(OAuthConfig.get("crm." + module.toLowerCase() + ".endpoint"), json);
		} else {
			LOGGER.info("Generating CSV for bulk update for module {} with {} records", module, updates.size());
			BulkWriteService.generateCsvFiles(module, updates);
		}
	}

	public void stopScheduler() {
		LOGGER.info("Stopping password update scheduler...");
		SCHEDULER.shutdown();
		try {
			if (!SCHEDULER.awaitTermination(10, TimeUnit.SECONDS)) {
				SCHEDULER.shutdownNow();
				LOGGER.warn("Scheduler forced shutdown due to timeout.");
			} else {
				LOGGER.info("Scheduler stopped successfully.");
			}
		} catch (InterruptedException e) {
			SCHEDULER.shutdownNow();
			Thread.currentThread().interrupt();
			LOGGER.error("Scheduler interrupted during shutdown: {}", e.getMessage(), e);
		}
	}

}