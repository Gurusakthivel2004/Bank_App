package crm;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import util.FileUtils;
import util.OAuthConfig;

public class BulkWriteService {

	private static final Logger logger = LogManager.getLogger(BulkWriteService.class);
	private static final String UPLOAD_URL = OAuthConfig.get("crm.uploadfile.endpoint");

	public static void initiateBulkUpload(String endpoint, List<Map<String, Object>> dataList, String moduleAPIName,
			String callbackUrl) throws Exception {

		if (dataList == null || dataList.isEmpty()) {
			logger.warn("Data list is empty. Aborting bulk upload.");
			return;
		}

		File zipFile = FileUtils.generateCsvFiles(moduleAPIName, dataList);
		if (zipFile == null || !zipFile.exists()) {
			logger.error("Failed to generate CSV ZIP file for module {}", moduleAPIName);
			return;
		}

		String fileId = CRMHttpService.getInstance().uploadFileToCrm(zipFile, UPLOAD_URL);
		logger.info("Uploaded ZIP file to Zoho. Received file_id = {}", fileId);

		String requestPayload = buildBulkJobPayload(moduleAPIName, fileId, callbackUrl);

		String response = CRMHttpService.getInstance().postToCrm(endpoint, requestPayload);
		logger.info("Bulk upload job created successfully. Response: {}", response);
	}

	private static String buildBulkJobPayload(String module, String fileId, String callbackUrl) {
		return String.format(
				"{\n" + "  \"operation\": \"update\",\n" + "  \"ignore_empty\": true,\n" + "  \"resource\": [\n"
						+ "    {\n" + "      \"type\": \"data\",\n" + "      \"module\": \"%s\",\n"
						+ "      \"file_id\": \"%s\"\n" + "    }\n" + "  ],\n" + "  \"callback\": {\n"
						+ "    \"url\": \"%s\",\n" + "    \"method\": \"post\"\n" + "  }\n" + "}",
				module, fileId, callbackUrl);
	}
}
