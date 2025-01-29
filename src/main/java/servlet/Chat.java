package servlet;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.type.TypeReference;

import cache.CacheUtil;

@ServerEndpoint("/chat")
public class Chat {

	private static final CopyOnWriteArrayList<Session> employees = new CopyOnWriteArrayList<>();
	private static final CopyOnWriteArrayList<Session> users = new CopyOnWriteArrayList<>();
	private static final Map<Session, Session> employeeToUserMap = new HashMap<>();

	private final Logger logger = LogManager.getLogger(Chat.class);
	private static final CacheUtil cacheUtil = new CacheUtil();
	private static final String USER_QUEUE_KEY = "userQueue";

	@OnOpen
	public void onOpen(Session session) {
		logger.info("New connection opened: " + session.getId());
	}

	@OnMessage
	public void onMessage(Session session, String message) {
		logger.info("Message received from session " + session.getId() + ": " + message);

		if (message.equals("employee")) {
			session.getUserProperties().put("role", "employee");
			employees.add(session);
			logger.info("Employee connected: " + session.getId());
			assignQueuedUser();
		} else if (message.equals("User requesting chat")) {
			session.getUserProperties().put("role", "user");
			users.add(session);
			logger.info("User connected: " + session.getId());
			queueUser(session);
		} else if (isEmployee(session)) {
			sendMessageToAssignedUser(session, message);
		} else {
			sendMessageToAssignedEmployee(session, message);
		}
	}

	@OnClose
	public void onClose(Session session) {

		logger.info("Connection closed: " + session.getId());

		if (isEmployee(session)) {
			employees.remove(session);
			Session assignedUser = employeeToUserMap.get(session);
			System.out.println("assigned User: " + assignedUser);
			if (assignedUser != null) {
				if (assignedUser.isOpen()) {
					sendMessageToUser(assignedUser, "Employee disconnected. Please start a new chat!");
//					queueUser(assignedUser);
				}
			}
		} else {
			users.remove(session);
			for (Session emp : employeeToUserMap.keySet()) {
				Session user = employeeToUserMap.get(emp);
				if (user.equals(session)) {
					sendMessageToEmployee(emp, "User disconnected.");
					employeeToUserMap.remove(emp);
				}
			}
			dequeueUser(session);

		}
	}

	private boolean isEmployee(Session session) {
		return "employee".equals(session.getUserProperties().get("role"));
	}

	private void queueUser(Session userSession) {
		List<String> userQueue = cacheUtil.get(USER_QUEUE_KEY, new TypeReference<List<String>>() {
		});
		if (userQueue == null) {
			userQueue = new CopyOnWriteArrayList<>();
		}
		userQueue.add(userSession.getId());
		cacheUtil.save(USER_QUEUE_KEY, userQueue);
		assignQueuedUser();
	}

	private void dequeueUser(Session userSession) {
		List<String> userQueue = cacheUtil.get(USER_QUEUE_KEY, new TypeReference<List<String>>() {
		});
		if (userQueue != null) {
			userQueue.remove(userSession.getId());
			cacheUtil.save(USER_QUEUE_KEY, userQueue);
		}
	}

	private void assignQueuedUser() {
		List<String> userQueue = cacheUtil.get(USER_QUEUE_KEY, new TypeReference<List<String>>() {
		});
		if (userQueue == null || userQueue.isEmpty()) {
			return;
		}

		for (Session employeeSession : employees) {
			if (!userQueue.isEmpty() && employeeToUserMap.get(employeeSession) == null) {
				String userId = userQueue.remove(0);
				Session userSession = users.stream().filter(session -> session.getId().equals(userId)).findFirst()
						.orElse(null);

				if (userSession != null) {
					employeeToUserMap.put(employeeSession, userSession);
					sendMessageToUser(userSession, "You are connected with an employee.");
					sendMessageToEmployee(employeeSession, "New user assigned: " + userSession.getId());
				}
			}

			cacheUtil.save(USER_QUEUE_KEY, userQueue);
			if (userQueue.isEmpty()) {
				break;
			}
		}
	}

	private void sendMessageToAssignedUser(Session employeeSession, String message) {
		Session assignedUser = employeeToUserMap.get(employeeSession);
		if (assignedUser != null) {
			if (assignedUser.isOpen()) {
				try {
					assignedUser.getBasicRemote().sendText(message);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} else {
			logger.info("No users assigned to employee: " + employeeSession.getId());
		}
	}

	private void sendMessageToAssignedEmployee(Session userSession, String message) {
		for (Map.Entry<Session, Session> entry : employeeToUserMap.entrySet()) {
			if (entry.getValue().equals(userSession)) {
				Session employeeSession = entry.getKey();
				if (employeeSession.isOpen()) {
					try {
						employeeSession.getBasicRemote().sendText(message);
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				break;
			}
		}
	}

	private void sendMessageToUser(Session userSession, String message) {
		try {
			if (userSession != null && userSession.isOpen()) {
				userSession.getBasicRemote().sendText(message);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void sendMessageToEmployee(Session employeeSession, String message) {
		try {
			if (employeeSession != null && employeeSession.isOpen()) {
				employeeSession.getBasicRemote().sendText(message);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}