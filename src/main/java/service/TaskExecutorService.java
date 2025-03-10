package service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dao.ActivityLogDAO;
import dao.DAO;
import model.ActivityLog;
import util.Helper;

public class TaskExecutorService {

	private static final int N_THREADS = 5;
	private static ExecutorService executor = Executors.newFixedThreadPool(N_THREADS);
	private DAO<ActivityLog> activityLogDAO = ActivityLogDAO.getInstance();
	private static Logger logger = LogManager.getLogger(TaskExecutorService.class);

	private TaskExecutorService() {}

	private static class SingletonHelper {
		private static final TaskExecutorService INSTANCE = new TaskExecutorService();
	}

	public static TaskExecutorService getInstance() {
		return SingletonHelper.INSTANCE;
	}

	public void submit(ActivityLog activityLog) throws Exception {
		if (activityLog.getPerformedBy() == null) {
			long userId = (Long) Helper.getThreadLocalValue("id");
			activityLog.setPerformedBy(userId);
		}
		activityLog.setTimestamp(System.currentTimeMillis());
		executor.submit(() -> {
			try {
				activityLogDAO.create(activityLog);
			} catch (Exception e) {
				logger.error("Error occurred while saving activity log: " + e.getMessage());

			}
		});
	}
}