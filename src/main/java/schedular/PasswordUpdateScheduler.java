package schedular;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import model.User;
import service.NotificationService;
import service.UserService;

public class PasswordUpdateScheduler {

	private static final Logger logger = LogManager.getLogger(PasswordUpdateScheduler.class);
	private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	private static final long DEFAULT_VERSION = 0;

	private final UserService userService = UserService.getInstance();
	private final NotificationService notificationService = NotificationService.getInstance();

	@SuppressWarnings("unchecked")
	public void startScheduler() {
		ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();

		Runnable task = () -> {
			Thread.currentThread().setContextClassLoader(contextClassLoader);

			logger.info("Password update check started...");

			try {
				Map<String, Object> userCriteria = new HashMap<>();
				userCriteria.put("passwordVersion", DEFAULT_VERSION);

				List<User> usersToNotify = (List<User>) userService.getUserDetails(userCriteria).get("users");

				if (usersToNotify.isEmpty()) {
					logger.info("No users need a password update.");
				} else {
					logger.info("Found {} users requiring a password update.", usersToNotify.size());

					for (User user : usersToNotify) {
						try {
							notificationService.sendEmail(user.getEmail(), "Update Your Password",
									"Your password is outdated. Please update it for security reasons.");
							logger.info("Email sent successfully to: {}", user.getEmail());
						} catch (Exception emailException) {
							logger.error("Failed to send email to {}: {}", user.getEmail(),
									emailException.getMessage());
						}
					}
				}
			} catch (Exception e) {
				logger.error("Error during password update check: {}", e.getMessage(), e);
			}

			logger.info("Password update check completed.");
		};

		scheduler.scheduleAtFixedRate(task, 0, 1, TimeUnit.DAYS);
		logger.info("Password update scheduler started: runs every 24 hours.");
	}

	public void stopScheduler() {
		logger.info("Stopping password update scheduler...");
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
