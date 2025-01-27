package controller;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonObject;

import model.Branch;
import service.BranchService;
import util.CustomException;
import util.Helper;

public class BranchController {

	private final BranchService branchService = new BranchService();
	private final Logger logger = LogManager.getLogger(BranchController.class);

	public void handlePost(HttpServletRequest request, HttpServletResponse response) throws IOException {
		try {
			logger.info("Received POST request to create a branch.");

			JsonObject jsonObject = Helper.parseRequestBody(request);
			logger.debug("Parsed request body: {}", jsonObject);

			Map<String, Object> branchMap = Helper.mapJsonObject(jsonObject);
			logger.debug("Mapped JSON to branchMap: {}", branchMap);

			branchService.createBranch(branchMap);
			logger.info("Branch created successfully. Branch details: {}", branchMap);

			Helper.sendSuccessResponse(response, "success");
		} catch (CustomException exception) {
			logger.warn("CustomException occurred while handling POST request: {}", exception.getMessage());
			Helper.sendErrorResponse(response, exception.getMessage());
		} catch (Exception exception) {
			logger.error("Unexpected error occurred while handling POST request.", exception);
			Helper.sendErrorResponse(response, "Unexpected error occurred while creating branch.");
		}
	}

	public void handleGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		logger.info("Received GET request to fetch branch details.");

		Map<String, Object> branchMap = Helper.getParametersAsMap(request);
		logger.debug("Extracted parameters from request: {}", branchMap);

		try {
			List<Branch> branches = branchService.getBranchDetails(branchMap);
			logger.info("Fetched branch details successfully. Total branches found: {}", branches.size());

			Helper.sendSuccessResponse(response, branches);
		} catch (CustomException exception) {
			logger.warn("CustomException occurred while handling GET request: {}", exception.getMessage());
			Helper.sendErrorResponse(response, exception.getMessage());
		} catch (Exception exception) {
			logger.error("Unexpected error occurred while handling GET request.", exception);
			Helper.sendErrorResponse(response, "Unexpected error occurred while fetching branch details.");
		}
	}
}
