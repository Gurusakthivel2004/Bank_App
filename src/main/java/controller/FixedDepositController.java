package controller;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonObject;

import service.FixedDepositService;
import util.Helper;

public class FixedDepositController {

	private static Logger logger = LogManager.getLogger(FixedDepositController.class);
	private FixedDepositService fdService = FixedDepositService.getInstance();

	private FixedDepositController() {}

	private static class SingletonHelper {
		private static final FixedDepositController INSTANCE = new FixedDepositController();
	}

	public static FixedDepositController getInstance() {
		return SingletonHelper.INSTANCE;
	}

	public void handleGet(HttpServletRequest request, HttpServletResponse response) throws Exception {
		Helper.sendSuccessResponse(response, "success");
	}

	public void handlePost(HttpServletRequest request, HttpServletResponse response) throws Exception {
		logger.info("Received POST request to create FD details.");

		JsonObject jsonObject = Helper.parseRequestBody(request);
		logger.debug("Parsed request body: {}", jsonObject);

		Map<String, Object> fdMap = Helper.mapJsonObject(jsonObject);
		logger.debug("Mapped JSON to fdMap: {}", fdMap);
		
		HttpSession session = request.getSession();
		fdService.createFixedDeposit(fdMap, session);
		logger.info("Fixed Deposit created successfully. Details: {}", fdMap);
		Helper.sendSuccessResponse(response, "success");
	}
}
