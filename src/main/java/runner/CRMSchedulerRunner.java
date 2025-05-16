package runner;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import cache.RedisCache;
import initializer.Initializer;
import redis.clients.jedis.Jedis;
import schedular.CRMSchedular;

public class CRMSchedulerRunner {

	public static ObjectMapper mapper = new ObjectMapper();
	public static RedisCache redisCache;

	public static void main(String[] args) throws Exception {
		Initializer.setDataSource();
		redisCache = RedisCache.getInstance();
		updateContactRecord();
//		updateAccountRecord();
		// Run scheduler processing once (not the periodic version)
		CRMSchedular schedular = new CRMSchedular();
		schedular.processUpdateSet();
	}

	private static void updateContactRecord() throws Exception {

		try (Jedis jedis = redisCache.getConnection()) {
			jedis.del("updateSet");
			for (int i = 1; i <= 10; i++) {
				Map<String, String> mockRecord = new HashMap<>();
				mockRecord.put("Module_Code", "1");
				mockRecord.put("Criteria_Key", "Email");
				mockRecord.put("Criteria_Value", "vijayguru2004@gmail.com");

				Map<String, String> updateFields = new HashMap<>();
				updateFields.put("Department", "ECE" + i);

				mockRecord.put("Update_Fields", mapper.writeValueAsString(updateFields));

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