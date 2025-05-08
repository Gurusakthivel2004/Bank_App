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

import cache.RedisCache;
import enums.Constants.FieldIdentifier;
import redis.clients.jedis.Jedis;
import service.CRMService;
import util.OAuthConfig;

public class CRMSchedular {

	private static final Logger logger = LogManager.getLogger(CRMSchedular.class);
	private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	private static RedisCache redisCache = RedisCache.getInstance();

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
		
		scheduler.scheduleAtFixedRate(task, 0, 1, TimeUnit.MINUTES);
		logger.info("Password update scheduler started: runs every 24 hours.");
	}

	@SuppressWarnings("unchecked")
	public void processUpdateSet() {
	    try (Jedis jedis = redisCache.getConnection()) {
	        long size = jedis.zcard("updateSet");
	        if (size == 0) {
	            logger.info("No records to update in updateSet.");
	            return;
	        }

	        List<String> entries = jedis.zrange("updateSet", 0, Math.min(size, 100) - 1);

	        List<Map<String, Object>> contactsData = new ArrayList<>();
	        List<Map<String, Object>> accountsData = new ArrayList<>();
	        List<Map<String, Object>> dealsData = new ArrayList<>();

	        for (String entry : entries) {
	            Map<String, Object> record = new ObjectMapper().readValue(entry, new TypeReference<Map<String, Object>>() {});
	            
	            if (record.containsKey("id")) {
	                Map<String, Object> cleanRecord = new HashMap<>();
	                String moduleType = null;

	                for (Map.Entry<String, Object> fieldEntry : record.entrySet()) {
	                    String fieldName = fieldEntry.getKey();
	                    Object fieldValue = fieldEntry.getValue();

	                    if (fieldValue instanceof Map) {
	                        Map<String, Object> valueMap = (Map<String, Object>) fieldValue;
	                        Object value = valueMap.get("value");
	                        Object identifier = valueMap.get("identifier");

	                        if (identifier != null) {
	                            int id = (Integer) identifier;
	                            moduleType = FieldIdentifier.getModuleForIdentifier(id); 
	                        }

	                        cleanRecord.put(fieldName, value);
	                    } else {
	                        cleanRecord.put(fieldName, fieldValue);
	                    }
	                }

	                if ("Contacts".equalsIgnoreCase(moduleType)) {
	                    contactsData.add(cleanRecord);
	                } else if ("Accounts".equalsIgnoreCase(moduleType)) {
	                    accountsData.add(cleanRecord);
	                } else if ("Deals".equalsIgnoreCase(moduleType)) {
	                	dealsData.add(cleanRecord);
	                } else {
	                    logger.warn("Unknown moduleType for record: {}", record);
	                }
	            }
	        }

	        ObjectMapper mapper = new ObjectMapper();
	        if (!contactsData.isEmpty()) {
	            Map<String, Object> contactsPayload = Collections.singletonMap("data", contactsData);
	            String contactsJson = mapper.writeValueAsString(contactsPayload);
	            logger.info("Json generated contact updates to CRM."+ contactsJson);
	            CRMService.getInstance().putToCrm(OAuthConfig.get("crm.contact.endpoint"), contactsJson);
	            logger.info("Sent {} contact updates to CRM.", contactsData.size());
	        }

	        if (!accountsData.isEmpty()) {
	            Map<String, Object> accountsPayload = Collections.singletonMap("data", accountsData);
	            String accountsJson = mapper.writeValueAsString(accountsPayload);
	            logger.info("Json generated account updates to CRM."+ accountsPayload);
	            CRMService.getInstance().putToCrm(OAuthConfig.get("crm.account.endpoint"), accountsJson);
	            logger.info("Sent {} account updates to CRM.", accountsData.size());
	        }
	        
	        if (!dealsData.isEmpty()) {
	            Map<String, Object> dealsPayload = Collections.singletonMap("data", dealsData);
	            String dealsJson = mapper.writeValueAsString(dealsPayload);
	            logger.info("Json generated deals updates to CRM."+ dealsPayload);
	            CRMService.getInstance().putToCrm(OAuthConfig.get("crm.deal.endpoint"), dealsJson);
	            logger.info("Sent {} deals updates to CRM.", accountsData.size());
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