package schedular;

import java.util.Collections;
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

import cache.RedisCache;
import crm.AccountsService;
import crm.CRMHttpService;
import crm.DealsService;
import enums.Constants.HttpStatusCodes;
import enums.Constants.UseCase;
import model.Org;
import model.User;
import redis.clients.jedis.Jedis;
import service.OrgService;
import service.UserService;
import util.Helper;

public class CRMInsertSchedular {

	private static final int MAXIMUM_RETRIES = 3;
	private static final Logger LOGGER = LogManager.getLogger(CRMInsertSchedular.class);

	private static final ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(1);

	private static final RedisCache REDIS_CACHE = RedisCache.getInstance();

	public void startScheduler() {
		ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();

		Runnable task = () -> {
			Thread.currentThread().setContextClassLoader(contextClassLoader);

			LOGGER.info("CRM insert started...");

			try {
				processInsertSet();
			} catch (Exception e) {
				LOGGER.error("Error during CRM insert: {}", e.getMessage(), e);
			}

			LOGGER.info("CRM insert completed.");
		};

		SCHEDULER.scheduleAtFixedRate(task, 0, 30, TimeUnit.MINUTES);
		LOGGER.info("CRM Insert scheduler started: runs every 24 hours.");
	}

	public void processInsertSet() {
		try (Jedis jedis = REDIS_CACHE.getConnection()) {
			List<String> entries = fetchEntriesFromInsertSet(jedis);
			if (entries.isEmpty())
				return;

			try {
				processEntries(entries);
			} catch (Exception e) {
				if (CRMHttpService.isForbidden(e) || CRMHttpService.isServerError(e)) {
					return;
				}
			}

			LOGGER.info("Processed and removed {} records from insertSet.", entries.size());

		} catch (Exception e) {
			LOGGER.error("Error processing insertSet: {}", e.getMessage(), e);
		}
	}

	private List<String> fetchEntriesFromInsertSet(Jedis jedis) {
		long size = jedis.zcard("insertSet");

		if (size == 0) {
			LOGGER.info("No records to insert in sortedset.");
			return Collections.emptyList();
		}

		LOGGER.info("Total records to process in insertSet: {}", size);

		List<String> entries = jedis.zrange("insertSet", 0, Math.min(size, 100) - 1);
		LOGGER.info("Fetched {} entries from insertSet for processing.", entries.size());

		return entries;
	}

	private void processEntries(List<String> entries) throws Exception {
		ObjectMapper mapper = new ObjectMapper();

		for (String entry : entries) {
			Map<String, Object> record = mapper.readValue(entry, new TypeReference<Map<String, Object>>() {
			});

			Integer retries = (Integer) record.get("retries");
			Integer useCaseId = Integer.parseInt((String) record.get("useCaseId"));

			UseCase useCase = UseCase.fromId(useCaseId);

			LOGGER.debug("Processing record for usecase: {}", useCase);

			if (retries > MAXIMUM_RETRIES) {
				removeFromInsertSet(entry);
				continue;
			}
			insertRecord(useCase, entry);
		}
	}

	private void insertRecord(UseCase useCase, String entry) throws Exception {
		ObjectMapper mapper = new ObjectMapper();

		Map<String, String> entryMap = mapper.readValue(entry, new TypeReference<Map<String, String>>() {
		});
		try {
			switch (useCase) {
			case ORG_PUSH:
				handleOrgPush(entryMap);
				break;
			case DEAL_PUSH:
				handleDealPush(entryMap);
				break;
			default:
				LOGGER.info("Invalid use case.");
				break;
			}
			removeFromInsertSet(entry);
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
			e.printStackTrace();
			LOGGER.error("CRM error JSON: {}", jsonPart);
			handleFailedRequests(jsonPart, entry, useCase);
		}

	}

	public boolean handleOrgPush(Map<String, String> requestMap) throws Exception {
		Long orgId = Long.parseLong(requestMap.get("orgId"));
		Long userId = Long.parseLong(requestMap.get("userId"));

		User user = UserService.getInstance().getUserById(userId);
		Org org = OrgService.getInstance().getOrgById(orgId);

		String accountId = AccountsService.getInstance().pushOrgToCRM(org, user, false);
		return accountId == null;
	}

	public boolean handleDealPush(Map<String, String> requestMap) throws Exception {
		Long orgId = Long.parseLong(requestMap.get("orgId"));
		Long userId = Long.parseLong(requestMap.get("userId"));

		String moduleRecordId = requestMap.get("moduleRecordId");
		String amount = requestMap.get("amount");
		String moduleName = requestMap.get("moduleName");

		User user = UserService.getInstance().getUserById(userId);
		Org org = OrgService.getInstance().getOrgById(orgId);

		Long dealId = DealsService.getInstance().pushModuleToCRM(moduleName, amount, moduleRecordId, user, org, false);

		return dealId == null;
	}

	private void removeFromInsertSet(String entry) {
		try (Jedis jedis = REDIS_CACHE.getConnection()) {
			jedis.zrem("insetSet", entry);
			LOGGER.info("Removed entry {} from updateSet", entry);
		}
	}

	private void handleFailedRequests(String response, String entry, UseCase useCase) {
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
				String status = item.path("status").asText();

				if (status.equals("success")) {
					removeFromInsertSet(entry);
				} else if (status.equals("error")) {
					String code = item.path("code").asText();
					Optional<HttpStatusCodes> codeOpt = HttpStatusCodes.fromName(code);

					codeOpt.ifPresent(statusCode -> {
						switch (statusCode) {
						case INVALID_DATA:
						case INVALID_MODULE:
						case INVALID_REQUEST_METHOD:
							persistToDb(entry, useCase);
							removeFromInsertSet(entry);
							break;

						case INTERNAL_ERROR:
						case FORBIDDEN:
							LOGGER.debug("Server error. skipping this json");
							break;
						default:
							break;
						}
					});

				}
			}

		} catch (Exception e) {
			LOGGER.error("Error while parsing CRM response: {}", e.getMessage(), e);
		}
	}

	private void persistToDb(String entry, UseCase useCase) {
		ObjectMapper mapper = new ObjectMapper();
		LOGGER.warn("Request failed. Logging the failed request.");
		try {
			Map<String, Object> record = mapper.readValue(entry, new TypeReference<Map<String, Object>>() {
			});

			Integer retries = (Integer) record.get("retries");
			record.put("retries", retries + 1);

			Helper.logFailedRequest(record);
		} catch (Exception exception) {
			LOGGER.error("Error occurred: " + exception);
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
