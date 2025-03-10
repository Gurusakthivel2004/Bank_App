package controller;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonObject;

import model.Branch;
import service.BranchService;
import util.Helper;

public class BranchController {

	private BranchController() {}

	private static class SingletonHelper {
		private static final BranchController INSTANCE = new BranchController();
	}

	public static BranchController getInstance() {
		return SingletonHelper.INSTANCE;
	}

	private BranchService branchService = BranchService.getInstance();
	private static Logger logger = LogManager.getLogger(BranchController.class);

	public void handlePost(HttpServletRequest request, HttpServletResponse response) throws Exception {
		logger.info("Received POST request to create a branch.");

		JsonObject jsonObject = Helper.parseRequestBody(request);
		logger.debug("Parsed request body: {}", jsonObject);

		Map<String, Object> branchMap = Helper.mapJsonObject(jsonObject);
		logger.debug("Mapped JSON to branchMap: {}", branchMap);

		branchService.createBranch(branchMap);
		logger.info("Branch created successfully. Branch details: {}", branchMap);

		Helper.sendSuccessResponse(response, "success");
	}

	public void handleGet(HttpServletRequest request, HttpServletResponse response) throws Exception {
		logger.info("Received GET request to fetch branch details.");

		Map<String, Object> branchMap = Helper.getParametersAsMap(request);
		logger.debug("Extracted parameters from request: {}", branchMap);

		List<Branch> branches = branchService.getBranchDetails(branchMap);
		logger.info("Fetched branch details successfully. Total branches found: {}", branches.size());

		Helper.sendSuccessResponse(response, branches);
	}
}
