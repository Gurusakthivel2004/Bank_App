package controller;

import java.io.IOException;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonObject;
import service.TransactionService;
import util.CustomException;
import util.Helper;

public class TransactionController {

	private TransactionService transactionService = TransactionService.getInstance();
	private final Logger logger = LogManager.getLogger(TransactionController.class);

	public void handleGet(HttpServletRequest request, HttpServletResponse response, Map<String, Object> txMap)
			throws IOException {
		try {
			logger.info("Received GET request to fetch transactions with parameters: {}", txMap);

			Object transactions = transactionService.getTransactionDetails(txMap);
			logger.info("Successfully fetched transactions: {}", transactions);

			Helper.sendSuccessResponse(response, transactions);
		} catch (CustomException exception) {
			logger.warn("CustomException occurred while fetching transactions: {}", exception.getMessage());
			Helper.sendErrorResponse(response, exception);
		} catch (Exception exception) {
			logger.error("Unexpected error occurred while fetching transactions.", exception);
			Helper.sendErrorResponse(response, "Unexpected error occurred while fetching transactions.");
		}
	}

	public void handlePost(HttpServletRequest request, HttpServletResponse response) throws IOException {
		try {
			logger.info("Received POST request for transaction processing.");

			JsonObject jsonObject = Helper.parseRequestBody(request);
			logger.debug("Parsed request body: {}", jsonObject);

			Map<String, Object> transactionMap = Helper.mapJsonObject(jsonObject);
			logger.debug("Mapped JSON to transactionMap: {}", transactionMap);

			if (transactionMap.containsKey("get")) {
				logger.info("Delegating to handleGet for processing.");
				handleGet(request, response, transactionMap);
				return;
			}

			transactionService.prepareTransaction(transactionMap);
			logger.info("Transaction successfully processed.");
			Helper.sendSuccessResponse(response, "success");
		} catch (CustomException exception) {
			logger.error("CustomException occurred while processing transaction: {}", exception.getMessage());
			Helper.sendErrorResponse(response, exception);
		} catch (Exception exception) {
			logger.error("Unexpected error occurred while processing transaction.", exception);
			Helper.sendErrorResponse(response, "Unexpected error occurred while processing transaction.");
		}
	}

}
