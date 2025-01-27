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
import util.CustomException;
import util.Helper;

public class AccountController {

	AccountService accountService = new AccountService();
	private final Logger logger = LogManager.getLogger(AccountController.class);

	public void handleGet(HttpServletRequest request, HttpServletResponse response, Map<String, Object> accountMap)
			throws IOException {
		try {
			logger.info("Processing GET request for account details with accountMap: {}", accountMap);
			Object accounts = accountService.getAccountDetails(accountMap);
			Helper.sendSuccessResponse(response, accounts);
			logger.info("Successfully fetched account details for accountMap: {}", accountMap);
		} catch (CustomException e) {
			logger.warn("CustomException occurred while fetching account details: {}", e.getMessage());
			Helper.sendErrorResponse(response, e);
		} catch (Exception e) {
			logger.error("Unexpected error occurred while fetching account details. AccountMap: {}", accountMap, e);
			Helper.sendErrorResponse(response, "Unexpected Error occurred while fetching accounts.");
		}
	}

	public void handleGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		handleGet(request, response, Helper.getParametersAsMap(request));
	}

	public void handlePost(HttpServletRequest request, HttpServletResponse response) throws IOException {
		try {
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
		} catch (CustomException exception) {
			logger.warn("CustomException occurred while handling POST request: {}", exception.getMessage());
			Helper.sendErrorResponse(response, exception);
		} catch (Exception exception) {
			logger.error("Unexpected error occurred while handling POST request.", exception);
			Helper.sendErrorResponse(response, "Unexpected error occurred while creating accounts.");
		}
	}

	public void handlePut(HttpServletRequest request, HttpServletResponse response)
			throws IOException, CustomException {
		try {
			logger.info("Received PUT request to update account details.");

			JsonObject jsonObject = Helper.parseRequestBody(request);
			logger.debug("Parsed request body: {}", jsonObject);

			Map<String, Object> accountMap = Helper.mapJsonObject(jsonObject);
			logger.debug("Mapped JSON to accountMap: {}", accountMap);

			if (!accountMap.containsKey("accountNumber")) {
				logger.warn("PUT request missing required 'accountNumber'.");
				Helper.sendErrorResponse(response, "Enter accountNumber to update account.");
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
		} catch (CustomException exception) {
			logger.warn("CustomException occurred while handling PUT request: {}", exception.getMessage());
			Helper.sendErrorResponse(response, exception);
		} catch (Exception exception) {
			logger.error("Unexpected error occurred while handling PUT request.", exception);
			Helper.sendErrorResponse(response, "Unexpected error occurred while updating accounts.");
		}
	}

}
