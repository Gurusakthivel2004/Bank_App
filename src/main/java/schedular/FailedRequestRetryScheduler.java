package schedular;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import crm.CRMHttpService;
import dao.DAO;
import dao.DaoFactory;
import enums.Constants.RetryStatus;
import model.FailedRequest;
import model.OauthProvider;
import util.Helper;
import util.HttpUtil;
import util.JsonUtils;

public class FailedRequestRetryScheduler {

	private static final Logger logger = LogManager.getLogger(FailedRequestRetryScheduler.class);
	private static final DAO<FailedRequest> FAILED_REQUEST_DAO = DaoFactory.getDAO(FailedRequest.class);
	private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	private static final String PROVIDER = "Zoho";

	private RetryStatus sendFailedRequest(FailedRequest failedRequest) throws Exception {
		OauthProvider provider = Helper.fetchOauthProvider(PROVIDER);
		
		String response = HttpUtil.sendRequest(failedRequest, provider);
		String code = JsonUtils.getValueByPath(response, "code", "");
		
		RetryStatus retryStatus = RetryStatus.fromString(code);
		return retryStatus;
	}
	
	private void deleteRequest(Long id) throws Exception {
		Map<String, Object> failedRequestCriteria = new HashMap<>();
		failedRequestCriteria.put("id", id);
		
		FAILED_REQUEST_DAO.update(null, failedRequestCriteria);
	}

	public void startScheduler() {
		Runnable task = () -> {
			logger.info("Retrying failed requests started...");

			try {
				List<FailedRequest> failedRequests = FAILED_REQUEST_DAO.get(new HashMap<String, Object>());

				if (failedRequests.isEmpty()) {
					logger.info("No failed requests to retry.");
					return;
				}

				for (FailedRequest request : failedRequests) {
					RetryStatus retryStatus = sendFailedRequest(request);

					if (retryStatus == RetryStatus.SUCCESS) {
						deleteRequest(request.getId());
						logger.info("Successfully resent and deleted request ID: {}", request.getId());
					} else {
						logger.warn("Retry failed for request ID: {}. Will retry later.", request.getId());
						return;
					}
				}

			} catch (Exception e) {
				if (CRMHttpService.isForbidden(e)) {
					logger.warn("retry attempt failed. Skipping batch retry. Will retry after 30 minutes.");
				}
				logger.error("Error during retrying failed requests: {}", e.getMessage(), e);
			}

			logger.info("Retrying failed requests completed.");
		};

		scheduler.scheduleAtFixedRate(task, 0, 30, TimeUnit.MINUTES);
		logger.info("FailedRequestRetryScheduler started: runs every 30 minutes.");
	}

	public void stopScheduler() {
		logger.info("Stopping FailedRequestRetryScheduler...");
		scheduler.shutdown();
		try {
			if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
				scheduler.shutdownNow();
				logger.warn("Scheduler forced shutdown due to timeout.");
			} else {
				logger.info("Scheduler stopped successfully.");
			}
		} catch (InterruptedException e) {
			scheduler.shutdownNow();
			Thread.currentThread().interrupt();
			logger.error("Scheduler interrupted during shutdown: {}", e.getMessage(), e);
		}
	}
}
