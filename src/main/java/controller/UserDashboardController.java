package controller;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import service.FacadeHandler;
import util.CustomException;
import util.Helper;

public class UserDashboardController {

	private final Logger logger = LogManager.getLogger(UserDashboardController.class);

	public void handleGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		logger.info("Received GET request to fetch dashboard details.");

		try {
			// Creating a FacadeHandler instance to handle the request
			FacadeHandler facadeHandler = new FacadeHandler();
			Map<String, Object> data = facadeHandler.dashBoardDetails();
			Helper.sendSuccessResponse(response, data);
			logger.info("Successfully fetched dashboard details.");
		} catch (CustomException e) {
			logger.error("CustomException occurred while fetching dashboard details: {}", e.getMessage(), e);
			Helper.sendErrorResponse(response, e);
		} catch (Exception exception) {
			logger.error("Unexpected error occurred while fetching dashboard details: {}", exception.getMessage(),
					exception);
			Helper.sendErrorResponse(response, "Unexpected error occurred while fetching details.");
		}
	}
}