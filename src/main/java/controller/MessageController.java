package controller;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonObject;

import service.MessageService;
import util.Helper;

public class MessageController {

	private MessageService messageService = MessageService.getInstance();
	private static Logger logger = LogManager.getLogger(MessageController.class);
	
	private MessageController() {}

	private static class SingletonHelper {
		private static final MessageController INSTANCE = new MessageController();
	}

	public static MessageController getInstance() {
		return SingletonHelper.INSTANCE;
	}

	public void handleGet(HttpServletRequest request, HttpServletResponse response, Map<String, Object> messageMap)
			throws Exception {
		logger.info("Processing GET request for message details with messageMap: {}", messageMap);
		Map<String, Object> messages = messageService.getMessageDetails(messageMap);
		Helper.sendSuccessResponse(response, messages);
		logger.info("Successfully fetched message details for messageMap: {}", messageMap);
	}

	public void handleGet(HttpServletRequest request, HttpServletResponse response) throws Exception {
		handleGet(request, response, Helper.getParametersAsMap(request));
	}

	public void handlePost(HttpServletRequest request, HttpServletResponse response) throws Exception {
		logger.info("Received POST request to create or fetch message details.");

		JsonObject jsonObject = Helper.parseRequestBody(request);
		logger.debug("Parsed request body: {}", jsonObject);

		Map<String, Object> messageMap = Helper.mapJsonObject(jsonObject);
		logger.debug("Mapped JSON to messageMap: {}", messageMap);

		if (messageMap.containsKey("get")) {
			logger.info("GET operation detected in POST request. Delegating to handleGet.");
			handleGet(request, response, messageMap);
			return;
		}

		messageService.createMessage(messageMap);
		logger.info("Message created successfully. Message details: {}", messageMap);
		Helper.sendSuccessResponse(response, "success");
	}

	public void handlePut(HttpServletRequest request, HttpServletResponse response) throws Exception {
		logger.info("Received PUT request to update message details.");

		JsonObject jsonObject = Helper.parseRequestBody(request);
		logger.debug("Parsed request body: {}", jsonObject);

		Map<String, Object> messageMap = Helper.mapJsonObject(jsonObject);
		logger.debug("Mapped JSON to messageMap: {}", messageMap);

		messageService.updateMessage(messageMap);
		Helper.sendSuccessResponse(response, "success");
	}

}