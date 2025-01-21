package service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dao.ActivityLogDAO;
import dao.DAO;
import model.ActivityLog;
import util.CustomException;
import util.Helper;

public class TaskExecutorService {

	private final ExecutorService executor = Executors.newSingleThreadExecutor();

	private final DAO<ActivityLog> activityLogDAO = new ActivityLogDAO();

	private TaskExecutorService() {
	}

	private static class SingletonHelper {
		private static final TaskExecutorService INSTANCE = new TaskExecutorService();
	}

	public static TaskExecutorService getInstance() {
		return SingletonHelper.INSTANCE;
	}

	public void submit(ActivityLog activityLog) {
		Long userId = (Long) Helper.getThreadLocalValue("id");

		activityLog.setTimestamp(System.currentTimeMillis()).setPerformedBy(userId);

		executor.submit(() -> {
			try {
				activityLogDAO.create(activityLog);
			} catch (CustomException e) {
				System.err.println("Error occurred while saving activity log: " + e.getMessage());
				throw new RuntimeException(e);
			}
		});
	}

}
