package runner;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import cache.RedisCache;
import initializer.Initializer;
import redis.clients.jedis.Jedis;
import schedular.CRMUpdateSchedular;
import schedular.FailedRequestRetryScheduler;

public class CRMSchedulerRunner {

	public static ObjectMapper mapper = new ObjectMapper();
	public static RedisCache redisCache;

	public static void main(String[] args) {
		try {
			Initializer.setDataSource();
			redisCache = RedisCache.getInstance();
//			OauthService.getInstance().refreshAccessToken();
//		updateContactRecord();
//		updateAccountRecord();	
			// Run scheduler processing once (not the periodic version)
			CRMUpdateSchedular schedular = new CRMUpdateSchedular();
			schedular.processUpdateSet();

			FailedRequestRetryScheduler fschedular = new FailedRequestRetryScheduler();
//			fschedular.startScheduler();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void updateContactRecord() throws Exception {

		try (Jedis jedis = redisCache.getConnection()) {
			jedis.del("updateSet");
			for (int i = 1; i <= 10; i++) {
				Map<String, Object> mockRecord = new HashMap<>();
				mockRecord.put("moduleCodeId", "1");
				mockRecord.put("criteriaKey", "Email");
				mockRecord.put("retries", 0);
				mockRecord.put("criteriaValue", "vijayguru2004@gmail.com");

				Map<String, Object> updateFields = new HashMap<>();
				updateFields.put("Department", i);

				mockRecord.put("updateFields", mapper.writeValueAsString(updateFields));

				String recordJson = mapper.writeValueAsString(mockRecord);

				jedis.zadd("updateSet", 1, recordJson);
			}
		}
		System.out.println("Inserted 10 contact mock CRM update records into Redis.");
	}

	private static void updateAccountRecord() throws Exception {
		try (Jedis jedis = redisCache.getConnection()) {

			Map<String, String> mockRecord = new HashMap<>();
			mockRecord.put("Module_Code", "0");
			mockRecord.put("Criteria_Key", "Account_Name");
			mockRecord.put("Criteria_Value", "Zoho");

			Map<String, String> updateFields = new HashMap<>();
			updateFields.put("Account_Site", "Branch");

			mockRecord.put("Update_Fields", mapper.writeValueAsString(updateFields));

			String recordJson = mapper.writeValueAsString(mockRecord);

			jedis.zadd("updateSet", 1, recordJson);
		}
		System.out.println("Inserted account mock CRM update records into Redis.");
	}
}