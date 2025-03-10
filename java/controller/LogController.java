package controller;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonObject;

import enums.Constants.HttpStatusCodes;
import service.ActivityLogService;
import util.CustomException;
import util.Helper;

public class LogController {
	
	private LogController() {}

	private static class SingletonHelper {
		private static final LogController INSTANCE = new LogController();
	}

	public static LogController getInstance() {
		return SingletonHelper.INSTANCE;
	}

	private ActivityLogService logService = ActivityLogService.getInstance();
	private static Logger logger = LogManager.getLogger(LogController.class);

	public void handleGet(HttpServletRequest request, HttpServletResponse response, Map<String, Object> logMap)
			throws Exception {

		logger.info("Received GET request to fetch logs with parameters: {}", logMap);

		Map<String, Object> logs = logService.getLogDetails(logMap);
		logger.info("Successfully fetched logs: {}", logs);

		Helper.sendSuccessResponse(response, logs);
	}

	public void handlePost(HttpServletRequest request, HttpServletResponse response) throws Exception {
		logger.info("Received POST request to fetch logs.");

		JsonObject jsonObject = Helper.parseRequestBody(request);
		logger.debug("Parsed request body: {}", jsonObject);

		Map<String, Object> logMap = Helper.mapJsonObject(jsonObject);
		logger.debug("Mapped JSON to logMap: {}", logMap);

		if (!logMap.containsKey("get")) {
			throw new CustomException("Invalid payload data. Please check your inputs", HttpStatusCodes.BAD_REQUEST);
		}

		handleGet(request, response, logMap);
	}

}