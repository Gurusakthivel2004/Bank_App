package controller;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonObject;

import service.ActivityLogService;
import util.CustomException;
import util.Helper;

public class MessageController {

	ActivityLogService logService = new ActivityLogService();
	private final Logger logger = LogManager.getLogger(MessageController.class);

	public void handleGet(HttpServletRequest request, HttpServletResponse response, Map<String, Object> logMap)
			throws IOException {
		try {
			logger.info("Received GET request to fetch logs with parameters: {}", logMap);

			Object logs = logService.getLogDetails(logMap);
			logger.info("Successfully fetched logs: {}", logs);

			Helper.sendSuccessResponse(response, logs);
		} catch (CustomException e) {
			logger.warn("CustomException occurred while fetching logs: {}", e.getMessage());
			Helper.sendErrorResponse(response, e);
		} catch (Exception e) {
			logger.error("Unexpected error occurred while fetching logs.", e);
			Helper.sendErrorResponse(response, "Unexpected Error occurred while fetching logs.");
		}
	}

	public void handlePost(HttpServletRequest request, HttpServletResponse response) throws IOException {
		logger.info("Received POST request to fetch logs.");

		JsonObject jsonObject = Helper.parseRequestBody(request);
		logger.debug("Parsed request body: {}", jsonObject);

		Map<String, Object> logMap = Helper.mapJsonObject(jsonObject);
		logger.debug("Mapped JSON to logMap: {}", logMap);

		handleGet(request, response, logMap);
	}

}
