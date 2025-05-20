package schedular;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import crm.AccountsService;
import crm.CRMHttpService;
import crm.DealsService;
import dao.DAO;
import dao.DaoFactory;
import enums.Constants.UseCase;
import model.Criteria;
import model.FailedRequest;
import model.Org;
import model.User;
import service.OrgService;
import service.UserService;
import util.CRMQueueManager;
import util.SQLHelper;

public class FailedRequestRetryScheduler {

	private static final Logger LOGGER = LogManager.getLogger(FailedRequestRetryScheduler.class);
	private static final DAO<FailedRequest> FAILED_REQUEST_DAO = DaoFactory.getDAO(FailedRequest.class);
	private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

	private void deleteRequest(List<Object> requestsIds) throws Exception {
		Criteria criteria = new Criteria().setClazz(FailedRequest.class).setColumn(Collections.singletonList("id"))
				.setOperator(Collections.singletonList("IN")).setValues(requestsIds);

		SQLHelper.delete(criteria);
		LOGGER.info("Deleted {} matured fixed deposits.", requestsIds.size());
	}

	public void startScheduler() {
		Runnable task = () -> {
			LOGGER.info("Retrying failed requests started...");
			List<Object> requestIds = new ArrayList<>();
			try {
				List<FailedRequest> failedRequests = FAILED_REQUEST_DAO.get(new HashMap<String, Object>());

				if (failedRequests.isEmpty()) {
					LOGGER.info("No failed requests to retry.");
					return;
				}
				ObjectMapper mapper = new ObjectMapper();
				for (FailedRequest request : failedRequests) {
					
					Map<String, String> requestMap = mapper.readValue(request.getRequestJson(),
							new TypeReference<Map<String, String>>() {
							});
					
					Integer useCaseId = Integer.parseInt(requestMap.get("useCase"));
					UseCase useCase = UseCase.fromId(useCaseId);
					boolean result = false;

					switch (useCase) {
					case ORG_PUSH:
						result = handleOrgPush(requestMap);
						break;
					case DEAL_PUSH:
						result = handleDealPush(requestMap);
						break;
					case CUSTOM_UPDATE:
						handleCustomUpdate(requestMap);
						result = true;
						break;
					default:
						LOGGER.debug("use case doesnot exists.");
					}

					if (result) {
						requestIds.add(request.getId());
						LOGGER.info("Successfully resent and deleted request ID: {}", request.getId());
					} else {
						LOGGER.warn("Retry failed for request ID: {}. Will retry later.", request.getId());
					}
				}
				deleteRequest(requestIds);
			} catch (Exception e) {
				if (CRMHttpService.isForbidden(e)) {
					LOGGER.warn("retry attempt failed. Skipping batch retry. Will retry after 30 minutes.");
				}
				LOGGER.error("Error during retrying failed requests: {}", e.getMessage(), e);
			}

			LOGGER.info("Retrying failed requests completed.");
		};

		scheduler.scheduleAtFixedRate(task, 0, 30, TimeUnit.MINUTES);
		LOGGER.info("FailedRequestRetryScheduler started: runs every 30 minutes.");
	}
	
	private void handleCustomUpdate(Map<String, String> requestMap) throws Exception {
		Integer moduleCodeId = Integer.parseInt(requestMap.get("moduleCodeId"));
		String updateJson = requestMap.get("updateJson");
		Integer retries = Integer.parseInt(requestMap.get("retries"));
		
		Map<String, Object> payload = new HashMap<>();
		payload.put("moduleCodeId", moduleCodeId.toString());
		payload.put("updateFields", updateJson);
		payload.put("retries", retries);
		
		CRMQueueManager.addToUpdateSet(payload);
		
	}

	private boolean handleOrgPush(Map<String, String> requestMap) throws Exception {
		Long orgId = Long.parseLong(requestMap.get("orgId"));
		Long userId = Long.parseLong(requestMap.get("userId"));

		User user = UserService.getInstance().getUserById(userId);
		Org org = OrgService.getInstance().getOrgById(orgId);

		String accountId = AccountsService.getInstance().pushOrgToCRM(org, user, false);
		return accountId == null;
	}

	private boolean handleDealPush(Map<String, String> requestMap) throws Exception {
		Long orgId = Long.parseLong(requestMap.get("orgId"));
		Long userId = Long.parseLong(requestMap.get("userId"));

		String moduleRecordId = requestMap.get("moduleRecordId");
		String amount = requestMap.get("amount");
		String moduleName = requestMap.get("moduleName");

		User user = UserService.getInstance().getUserById(userId);
		Org org = OrgService.getInstance().getOrgById(orgId);

		Long dealId = DealsService.getInstance().pushModuleToCRM(moduleName, amount, moduleRecordId, user, org, false);

		return dealId == null;
	}

	public void stopScheduler() {
		LOGGER.info("Stopping FailedRequestRetryScheduler...");
		scheduler.shutdown();
		try {
			if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
				scheduler.shutdownNow();
				LOGGER.warn("Scheduler forced shutdown due to timeout.");
			} else {
				LOGGER.info("Scheduler stopped successfully.");
			}
		} catch (InterruptedException e) {
			scheduler.shutdownNow();
			Thread.currentThread().interrupt();
			LOGGER.error("Scheduler interrupted during shutdown: {}", e.getMessage(), e);
		}
	}
}
