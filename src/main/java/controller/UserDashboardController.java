package controller;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import service.FacadeHandler;
import util.Helper;

public class UserDashboardController {

	private static Logger logger = LogManager.getLogger(UserDashboardController.class);
	
	private UserDashboardController() {}

	private static class SingletonHelper {
		private static final UserDashboardController INSTANCE = new UserDashboardController();
	}

	public static UserDashboardController getInstance() {
		return SingletonHelper.INSTANCE;
	}

	public void handleGet(HttpServletRequest request, HttpServletResponse response) throws Exception {
		logger.info("Received GET request to fetch dashboard details.");

		FacadeHandler facadeHandler = new FacadeHandler();
		Map<String, Object> data = facadeHandler.dashBoardDetails();
		Helper.sendSuccessResponse(response, data);
		logger.info("Successfully fetched dashboard details.");

	}
}