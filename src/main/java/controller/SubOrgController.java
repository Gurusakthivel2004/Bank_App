package controller;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonObject;

import model.SubOrg;
import service.SubOrgService;
import util.Helper;

public class SubOrgController {

	private static Logger logger = LogManager.getLogger(SubOrgController.class);
	private SubOrgService subOrgService = SubOrgService.getInstance();

	private SubOrgController() {}

	private static class SingletonHelper {
		private static final SubOrgController INSTANCE = new SubOrgController();
	}

	public static SubOrgController getInstance() {
		return SingletonHelper.INSTANCE;
	}

	public void handleGet(HttpServletRequest request, HttpServletResponse response) throws Exception {
		logger.info("Received GET request to fetch sub org details.");

		Map<String, Object> orgMap = Helper.getParametersAsMap(request);
		logger.debug("Extracted parameters from request: {}", orgMap);

		List<SubOrg> orgData = subOrgService.getSubOrgDetails(orgMap);
		logger.info("Fetched org details successfully. Total sub org data found: {}", orgData.size());

		Helper.sendSuccessResponse(response, orgData);
	}

	public void handlePost(HttpServletRequest request, HttpServletResponse response) throws Exception {
		logger.info("Received POST request to create sub org.");

		JsonObject jsonObject = Helper.parseRequestBody(request);
		logger.debug("Parsed request body: {}", jsonObject);

		Map<String, Object> orgMap = Helper.mapJsonObject(jsonObject);
		logger.debug("Mapped JSON to orgMap: {}", orgMap);

		subOrgService.createOrg(orgMap);
		logger.info("sub org created successfully. Details: {}", orgMap);
		Helper.sendSuccessResponse(response, "success");
	}
}