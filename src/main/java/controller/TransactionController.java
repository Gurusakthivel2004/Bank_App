package controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import service.TransactionService;
import util.CustomException;
import util.Helper;

public class TransactionController {
	
	TransactionService transactionService = new TransactionService();
	
	public void handleGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();

		Long id = Helper.parseLongOrDefault(request.getParameter("id"), 0L);
		Long accountNumber = Helper.parseLongOrDefault(request.getParameter("accountNumber"), 0L);
		Long from = Helper.parseDateToMillisOrDefault(request.getParameter("from"), 0L);
		Long to = Helper.parseDateToMillisOrDefault(request.getParameter("to"), 0L);
		Long limit = Helper.parseDateToMillisOrDefault(request.getParameter("limit"), 0L);
		
		try {
			if (id == -1) {
				id = (Long) Helper.getThreadLocalValue().get("id");
			}
			Object transactions = transactionService.getTransactionDetails(id, accountNumber, limit, from, to);
			String jsonResponse = new ObjectMapper().writeValueAsString(transactions);
			out.write(jsonResponse);
		} catch (CustomException e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			out.println(e.getMessage());
		} finally {
			out.close();
		}
	}

	public void handlePost(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.setContentType("application/json");
		PrintWriter out = response.getWriter();
		JsonObject responseJson = new JsonObject();

		try (BufferedReader reader = request.getReader()) {
			JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
			Map<String, Object> transactionMap = Helper.mapJsonObject(jsonObject);
			transactionService.createTransaction(transactionMap);
			responseJson.addProperty("message", "success");
			response.setStatus(HttpServletResponse.SC_OK);
		} catch (CustomException exception) {
			// Handle custom exception for failed account creation
			responseJson.addProperty("message", exception.getMessage());
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		} finally {
			out.print(responseJson.toString());
			out.close();
		}
	}
}
