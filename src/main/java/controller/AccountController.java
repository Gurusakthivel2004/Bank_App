package controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonObject;

import service.AccountService;
import util.Helper;

public class AccountController {

	private AccountService accountService = AccountService.getInstance();
	private static Logger logger = LogManager.getLogger(AccountController.class);

	private AccountController() {}

	private static class SingletonHelper {
		private static final AccountController INSTANCE = new AccountController();
	}

	public static AccountController getInstance() {
		return SingletonHelper.INSTANCE;
	}

	public void handleGet(HttpServletRequest request, HttpServletResponse response, Map<String, Object> accountMap)
			throws IOException, Exception {

		logger.info("Processing GET request for account details with accountMap: {}", accountMap);
		Object accounts = accountService.getAccountDetails(accountMap);
		Helper.sendSuccessResponse(response, accounts);
		logger.info("Successfully fetched account details for accountMap: {}", accountMap);

	}

	public void handleGet(HttpServletRequest request, HttpServletResponse response) throws Exception {
		handleGet(request, response, Helper.getParametersAsMap(request));
	}

	public void handlePost(HttpServletRequest request, HttpServletResponse response) throws Exception {

		logger.info("Received POST request to create or fetch account details.");

		JsonObject jsonObject = Helper.parseRequestBody(request);
		logger.debug("Parsed request body: {}", jsonObject);

		Map<String, Object> accountMap = Helper.mapJsonObject(jsonObject);
		logger.debug("Mapped JSON to accountMap: {}", accountMap);

		if (accountMap.containsKey("get")) {
			logger.info("GET operation detected in POST request. Delegating to handleGet.");
			handleGet(request, response, accountMap);
			return;
		}

		accountService.createAccount(accountMap);
		logger.info("Account created successfully. Account details: {}", accountMap);
		Helper.sendSuccessResponse(response, "success");

	}

	public void handlePut(HttpServletRequest request, HttpServletResponse response) throws Exception {

		logger.info("Received PUT request to update account details.");

		JsonObject jsonObject = Helper.parseRequestBody(request);
		logger.debug("Parsed request body: {}", jsonObject);

		Map<String, Object> accountMap = Helper.mapJsonObject(jsonObject);
		logger.debug("Mapped JSON to accountMap: {}", accountMap);

		if (!accountMap.containsKey("accountNumber")) {
			logger.warn("PUT request missing required 'accountNumber'.");
			Helper.sendErrorResponse(response, "Enter accountNumber to update account.", 400);
			return;
		}

		Long accountNumber = Long.parseLong((String) accountMap.get("accountNumber"));

		Map<String, Object> accMap = new HashMap<>();
		if (accountMap.containsKey("branchId")) {
			Long branchId = Long.parseLong((String) accountMap.get("branchId"));
			accMap.put("branchId", branchId);
		}
		if (accountMap.containsKey("status")) {
			String status = (String) accountMap.get("status");
			accMap.put("status", status);
		}

		accountService.updateAccount(accountNumber, accMap);
		logger.info("Account updated successfully. AccountNumber: {}, Updates: {}", accountNumber, accMap);
		Helper.sendSuccessResponse(response, "success");

	}

}