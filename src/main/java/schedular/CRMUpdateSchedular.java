package schedular;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import cache.CacheUtil;
import cache.RedisCache;
import crm.CRMHttpService;
import enums.Constants.HttpStatusCodes;
import enums.Constants.ModuleCode;
import enums.Constants.UseCase;
import redis.clients.jedis.Jedis;
import util.CRMQueueManager;
import util.FileUtils;
import util.Helper;
import util.JsonUtils;
import util.OAuthConfig;
import util.PhoneUtil;

public class CRMUpdateSchedular {

	private static final int RECORD_THRESHOLD = 15;
	private static final int MAXIMUM_RETRIES = 3;
	private static final Logger LOGGER = LogManager.getLogger(CRMUpdateSchedular.class);

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
		LOGGER.info("CRM Update scheduler started: runs every 24 hours.");
	}

	public void processUpdateSet() {
		try (Jedis jedis = REDIS_CACHE.getConnection()) {
			List<String> entries = fetchEntriesFromUpdateSet(jedis);
			if (entries.isEmpty())
				return;

			Map<String, List<Map<String, Object>>> moduleData = prepareModuleData(entries);
			try {
				processModuleData(moduleData);
			} catch (Exception e) {
				if (CRMHttpService.isForbidden(e) || CRMHttpService.isServerError(e)) {
					return;
				}
			}

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

	private Map<String, List<Map<String, Object>>> prepareModuleData(List<String> entries) throws Exception {
		Map<String, List<Map<String, Object>>> moduleData = new HashMap<>();
		ObjectMapper mapper = new ObjectMapper();

		for (String entry : entries) {
			Map<String, Object> record = mapper.readValue(entry, new TypeReference<Map<String, Object>>() {
			});

			Integer retries = (Integer) record.get("retries");
			Integer moduleCodeId = Integer.parseInt((String) record.get("moduleCodeId"));

			ModuleCode moduleCode = ModuleCode.fromId(moduleCodeId);
			String criteriaKey = (String) record.get("criteriaKey");
			String module = moduleCode.name();

			Object criteriaValue = record.get("criteriaValue");

			Integer inserted = CacheUtil.get(criteriaValue.toString(), new TypeReference<Integer>() {
			});

			if (inserted == 1) {
				continue;
			}

			LOGGER.debug("Processing record for module: {}, criteria: {}={}", module, criteriaKey, criteriaValue);

			Map<String, Object> updateFields = mapper.readValue(record.get("updateFields").toString(),
					new TypeReference<Map<String, Object>>() {
					});

			String recordId = (String) updateFields.get("id");
			if (recordId == null) {
				recordId = resolveRecordId(moduleCode, criteriaKey, criteriaValue);
			}

			if (retries > MAXIMUM_RETRIES) {
				removeFromUpdateSet(entry);
				continue;
			}

			if (updateFields.containsKey("phone")) {
				String phone = (String) updateFields.get("phone");
				String countryCode = (String) updateFields.get("countryCode");

				boolean isValidPhone = PhoneUtil.isValidPhoneNumber(phone, countryCode);
				if (!isValidPhone) {
					updateFields.remove("phone");
				}
			}

			updateFields.put("retries", retries);
			updateFields.put("id", recordId);

			LOGGER.debug("Update fields prepared for record {}: {}", recordId, updateFields);
			moduleData.computeIfAbsent(module, k -> new ArrayList<>()).add(updateFields);
		}

		return moduleData;
	}

	private String resolveRecordId(ModuleCode moduleCode, String criteriaKey, Object criteriaValue) throws Exception {
		String module = moduleCode.name();
		String recordId = CacheUtil.getCRMRecordId(module, criteriaValue);

		if (recordId != null) {
			LOGGER.info("Cache hit: Found recordId in cache for {}: {}", module, recordId);
			return recordId;
		}

		String endpoint = OAuthConfig.get("crm." + module.toLowerCase() + ".endpoint");
		String jsonResponse = CRM_HTTP_SERVICE.fetchRecord(endpoint, criteriaKey, criteriaValue);
		recordId = JsonUtils.getValueByPath(jsonResponse, "data[0]", "id");

		LOGGER.info("Fetched recordId from CRM for module {} and value {}: {}", module, criteriaValue, recordId);
		CacheUtil.saveCRMRecordId(module, criteriaValue, recordId);
		LOGGER.debug("Saved recordId to cache: {} -> {}", criteriaValue, recordId);

		return recordId;
	}

	private void processModuleData(Map<String, List<Map<String, Object>>> moduleData) throws Exception {
		List<Map<String, Object>> currentEntry = null;
		String currentModule = null;
		try {
			for (Map.Entry<String, List<Map<String, Object>>> entry : moduleData.entrySet()) {
				currentEntry = entry.getValue();
				currentModule = entry.getKey();
				processModuleUpdates(entry.getKey(), entry.getValue());
			}
		} catch (Exception e) {
			String fullMessage = e.getMessage();
			String jsonPart = fullMessage;

			int jsonStart = fullMessage.indexOf('{');
			if (jsonStart == -1) {
				jsonStart = fullMessage.indexOf('[');
			}

			if (jsonStart != -1) {
				jsonPart = fullMessage.substring(jsonStart);
			}

			LOGGER.error("CRM error JSON: {}", jsonPart);
			handleFailedRequests(jsonPart, currentEntry, currentModule);
		}
	}

	private void removeFromUpdateSet(String entry) {
		try (Jedis jedis = REDIS_CACHE.getConnection()) {
			jedis.zrem("updateSet", entry);
			LOGGER.info("Removed entry {} from updateSet", entry);
		}
	}

	private void handleFailedRequests(String response, List<Map<String, Object>> updateJson, String module) {
		ObjectMapper mapper = new ObjectMapper();
		try {
			JsonNode root = mapper.readTree(response);
			JsonNode dataArray = root.get("data");

			if (dataArray == null || !dataArray.isArray()) {
				LOGGER.warn("Unexpected response format or missing 'data' field");
				return;
			}

			for (int i = 0; i < dataArray.size(); i++) {
				JsonNode item = dataArray.get(i);
				JsonNode details = item.path("details");

				Map<String, Object> originalUpdate = updateJson.get(i);
				String status = item.path("status").asText();
				String recordId = details.path("id").asText();

				if ("success".equals(status)) {
					removeFromUpdateSet(recordId);
				} else if ("error".equals(status)) {
					handleErrorResponse(item, originalUpdate, recordId, module);
				}
			}

		} catch (Exception e) {
			LOGGER.error("Error while parsing CRM response: {}", e.getMessage(), e);
		}
	}

	private void handleErrorResponse(JsonNode item, Map<String, Object> originalUpdate, String recordId,
			String module) {
		String code = item.path("code").asText();
		Optional<HttpStatusCodes> codeOpt = HttpStatusCodes.fromName(code);

		codeOpt.ifPresent(statusCode -> {
			switch (statusCode) {
			case INVALID_DATA:
			case INVALID_MODULE:
			case INVALID_REQUEST_METHOD:
				persistToDb(originalUpdate, module);
				removeFromUpdateSet(recordId);
				break;

			case INTERNAL_ERROR:
			case FORBIDDEN:
				LOGGER.debug("Server error. Skipping this JSON.");
				break;

			default:
				LOGGER.debug("Unhandled error code: {}", code);
				break;
			}
		});
	}

	private void persistToDb(Map<String, Object> originalUpdate, String module) {
		LOGGER.warn("Request failed. Logging the failed request.");
		try {
			ModuleCode moduleCode = ModuleCode.fromName(module);

			Integer retries = (Integer) originalUpdate.remove("retries");
			String updateJson = new ObjectMapper().writeValueAsString(originalUpdate);

			Map<String, Object> jsonMap = new HashMap<>();

			jsonMap.put("useCase", UseCase.CUSTOM_UPDATE.getId().toString());
			jsonMap.put("updateJson", updateJson);
			jsonMap.put("moduleCodeId", moduleCode.getId().toString());
			jsonMap.put("retries", retries + 1);

			Helper.logFailedRequest(jsonMap);
		} catch (Exception exception) {
			exception.printStackTrace();
			LOGGER.error("error occurred: " + exception);
		}
	}

	private String processModuleUpdates(String module, List<Map<String, Object>> updates) throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		LOGGER.info("Total records for module {}: {}", module, updates.size());

		if (updates.size() <= RECORD_THRESHOLD) {
			Map<String, Object> payload = Collections.singletonMap("data", updates);
			String json = mapper.writeValueAsString(payload);

			LOGGER.info("Sending direct update to CRM for module {} with {} records", module, updates.size());
			LOGGER.debug("JSON Payload: {}", json);

			String response = CRM_HTTP_SERVICE.putToCrm(OAuthConfig.get("crm." + module.toLowerCase() + ".endpoint"),
					json);
			return response;

		} else {
			LOGGER.info("Generating CSV for bulk update for module {} with {} records", module, updates.size());
			FileUtils.generateCsvFiles(module, updates);
		}
		return null;
	}

	public void handleCustomUpdate(Map<String, String> requestMap) throws Exception {
		Integer moduleCodeId = Integer.parseInt(requestMap.get("moduleCodeId"));
		String updateJson = requestMap.get("updateJson");
		Integer retries = Integer.parseInt(requestMap.get("retries"));

		Map<String, Object> payload = new HashMap<>();
		payload.put("moduleCodeId", moduleCodeId.toString());
		payload.put("updateFields", updateJson);
		payload.put("retries", retries);

		CRMQueueManager.addToUpdateSet(payload);
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