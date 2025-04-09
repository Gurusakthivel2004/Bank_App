package controller;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonObject;

import service.TransactionService;
import util.Helper;

public class TransactionController {

	private TransactionService transactionService = TransactionService.getInstance();
	private static Logger logger = LogManager.getLogger(TransactionController.class);

	private TransactionController() {
	}

	private static class SingletonHelper {
		private static final TransactionController INSTANCE = new TransactionController();
	}

	public static TransactionController getInstance() {
		return SingletonHelper.INSTANCE;
	}

	public void handleGet(HttpServletRequest request, HttpServletResponse response, Map<String, Object> txMap)
			throws Exception {

		logger.info("Received GET request to fetch transactions with parameters: {}", txMap);

		Object transactions = transactionService.getTransactionDetails(txMap);
		logger.info("Successfully fetched transactions: {}", transactions);

		Helper.sendSuccessResponse(response, transactions);
	}

	public void handlePost(HttpServletRequest request, HttpServletResponse response) throws Exception {

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

		HttpSession session = request.getSession();

		long txId = transactionService.prepareTransaction(transactionMap, session);

		session.setAttribute("txId", txId);
		session.setAttribute("otpVerified", false);

		logger.info("Transaction successfully processed.");
		Helper.sendSuccessResponse(response, "success");
	}
}