package controller;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonObject;

import service.MessageService;
import util.CustomException;
import util.Helper;

public class MessageController {

	MessageService messageService = new MessageService();
	private final Logger logger = LogManager.getLogger(MessageController.class);

	public void handleGet(HttpServletRequest request, HttpServletResponse response, Map<String, Object> messageMap)
			throws IOException {
		try {
			logger.info("Processing GET request for message details with messageMap: {}", messageMap);
			Map<String, Object> messages = messageService.getMessageDetails(messageMap);
			Helper.sendSuccessResponse(response, messages);
			logger.info("Successfully fetched message details for messageMap: {}", messageMap);
		} catch (CustomException e) {
			logger.warn("CustomException occurred while fetching message details: {}", e);
			Helper.sendErrorResponse(response, e);
		} catch (Exception e) {
			logger.error("Unexpected error occurred while fetching message details. messageMap: {}", messageMap, e);
			Helper.sendErrorResponse(response, "Unexpected Error occurred while fetching message.");
		}
	}

	public void handleGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		handleGet(request, response, Helper.getParametersAsMap(request));
	}

	public void handlePost(HttpServletRequest request, HttpServletResponse response) throws IOException {
		try {
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
		} catch (CustomException exception) {
			logger.warn("CustomException occurred while handling POST request: {}", exception.getMessage());
			Helper.sendErrorResponse(response, exception);
		} catch (Exception exception) {
			logger.error("Unexpected error occurred while handling POST request.", exception);
			Helper.sendErrorResponse(response, "Unexpected error occurred while creating messages.");
		}
	}

	public void handlePut(HttpServletRequest request, HttpServletResponse response)
			throws IOException, CustomException {
		try {
			logger.info("Received PUT request to update message details.");

			JsonObject jsonObject = Helper.parseRequestBody(request);
			logger.debug("Parsed request body: {}", jsonObject);

			Map<String, Object> messageMap = Helper.mapJsonObject(jsonObject);
			logger.debug("Mapped JSON to messageMap: {}", messageMap);

			messageService.updateMessage(messageMap);
			Helper.sendSuccessResponse(response, "success");
		} catch (CustomException exception) {
			logger.warn("CustomException occurred while handling PUT request: {}", exception.getMessage());
			Helper.sendErrorResponse(response, exception);
		} catch (Exception exception) {
			logger.error("Unexpected error occurred while handling PUT request.", exception);
			Helper.sendErrorResponse(response, "Unexpected error occurred while updating messages.");
		}
	}

}
