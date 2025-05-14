package schedular;

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
import service.CRMService;
import util.JsonUtils;
import util.OAuthConfig;

public class CRMSchedular {

	private static final Logger logger = LogManager.getLogger(CRMSchedular.class);
	private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	private static RedisCache redisCache = RedisCache.getInstance();
	private static CRMHttpService crmHttpService = CRMHttpService.getInstance();
	private static final int RECORD_THRESHOLD = 100;

	public void startScheduler() {
		ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();

		Runnable task = () -> {
			Thread.currentThread().setContextClassLoader(contextClassLoader);

			logger.info("CRM update check started...");

			try {
				processUpdateSet();
			} catch (Exception e) {
				logger.error("Error during CRM update check: {}", e.getMessage(), e);
			}

			logger.info("Password update check completed.");
		};

		scheduler.scheduleAtFixedRate(task, 0, 30, TimeUnit.MINUTES);
		logger.info("Password update scheduler started: runs every 24 hours.");
	}

	public void processUpdateSet() {

		try (Jedis jedis = redisCache.getConnection()) {

			long size = jedis.zcard("updateSet");

			if (size == 0) {
				logger.info("No records to update in updateSet.");
				return;
			}

			List<String> entries = jedis.zrange("updateSet", 0, Math.min(size, 100) - 1);

			Map<String, List<Map<String, String>>> moduleData = new HashMap<>();

			for (String entry : entries) {
				Map<String, String> record = new ObjectMapper().readValue(entry,
						new TypeReference<Map<String, String>>() {
						});

				Integer moduleCodeId = Integer.parseInt(record.get("Module_Code"));
				ModuleCode moduleCode = ModuleCode.fromId(moduleCodeId);

				String criteriaKey = record.get("Criteria_Key");
				String criteriaValue = record.get("Criteria_Value");
				String module = moduleCode.name();

				String recordId = CacheUtil.getCRMRecordId(moduleCode.name(), criteriaValue);
				if (recordId == null) {
					String jsonResponse = crmHttpService.fetchRecord(criteriaKey, criteriaValue,
							OAuthConfig.get("crm." + module.toLowerCase() + ".endpoint"));

					recordId = JsonUtils.getValueByPath(jsonResponse, "data[0]", "id");
					System.out.println(JsonUtils.getValueByPath(jsonResponse, "data[0]", "id"));
					System.out.println(module + " " + criteriaValue + " " + recordId);
					CacheUtil.saveCRMRecordId(module, criteriaValue, recordId);
				}

				Map<String, String> updateFields = new ObjectMapper().readValue(record.get("Update_Fields"),
						new TypeReference<Map<String, String>>() {
						});

				updateFields.put("id", recordId);

				if (updateFields.containsKey("Phone")) {
					// handle phone validation
				}
				moduleData.get(module).add(updateFields);
			}

			ObjectMapper mapper = new ObjectMapper();

			for (String key : moduleData.keySet()) {
				if (moduleData.get(key).size() <= RECORD_THRESHOLD) {

					Map<String, Object> payload = Collections.singletonMap("data", moduleData.get(key));
					String json = mapper.writeValueAsString(payload);

					logger.info("Json generated contact updates to CRM." + json);
					CRMService.getInstance().putToCrm(OAuthConfig.get("crm." + key + ".endpoint"), json);
				} else {
					BulkWriteService.generateCsvFiles(key, moduleData.get(key));
				}
			}

			jedis.zremrangeByRank("updateSet", 0, Math.min(size, 100) - 1);
			logger.info("Processed and removed {} records from updateSet.", Math.min(size, 100));
		} catch (Exception e) {
			logger.error("Error processing updateSet: {}", e.getMessage(), e);
		}
	}

	public void stopScheduler() {
		logger.info("Stopping password update scheduler...");
		scheduler.shutdown();
		try {
			if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
				scheduler.shutdownNow();
				logger.warn("Scheduler forced shutdown due to timeout.");
			} else {
				logger.info("Scheduler stopped successfully.");
			}
		} catch (InterruptedException e) {
			scheduler.shutdownNow();
			Thread.currentThread().interrupt();
			logger.error("Scheduler interrupted during shutdown: {}", e.getMessage(), e);
		}
	}

}