package schedular;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import model.Criteria;
import model.UserSession;
import service.UserSessionService;
import util.SQLHelper;

public class ExpiredSessionSchedular {

	private static final Logger logger = LogManager.getLogger(ExpiredSessionSchedular.class);
	private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

	private final UserSessionService userSessionService = UserSessionService.getInstance();

	public void startScheduler() {
		Runnable task = () -> {
			logger.info("Delete expired sessions started...");

			try {
				Map<String, Object> sessionCriteria = new HashMap<>();
				List<UserSession> userSessions = userSessionService.getSessionDetails(sessionCriteria);

				if (userSessions.isEmpty()) {
					logger.info("No sessions exists.");
				} else {
					logger.info("Found {} sessions.", userSessions.size());
					List<Object> expiredSessions = new ArrayList<>();
					for (UserSession userSession : userSessions) {
						if (userSession.getExpiresAt() < System.currentTimeMillis()) {
							expiredSessions.add(userSession.getId());
						}
					}
					if(expiredSessions.isEmpty()) {
						logger.info("No expired sessions exists.");
						return;
					}
					Criteria criteria = new Criteria().setClazz(UserSession.class);
					criteria.setColumn(new ArrayList<String>(Arrays.asList("id")));
					criteria.setOperator(new ArrayList<String>(Arrays.asList("IN")));
					criteria.setValues(expiredSessions);
					SQLHelper.delete(criteria);
				}
			} catch (Exception e) {
				logger.error("Error during deleteing expired sessions: {}", e.getMessage(), e);
			}

			logger.info("Deleting expired sessions completed.");
		};

		scheduler.scheduleAtFixedRate(task, 0, 1, TimeUnit.DAYS);
		logger.info("scheduler started: runs every 24 hours.");
	}

	public void stopScheduler() {
		logger.info("Stopping expired session scheduler...");
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
