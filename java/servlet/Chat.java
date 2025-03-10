package servlet;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
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

	private static ConcurrentLinkedQueue<Session> EMPLOYEES = new ConcurrentLinkedQueue<>();
	private static ConcurrentLinkedQueue<Session> USERS = new ConcurrentLinkedQueue<>();
	private static Map<Session, Session> EMPLOYEE_TO_USER_MAP = new HashMap<>();

	private static Logger logger = LogManager.getLogger(Chat.class);
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
			EMPLOYEES.add(session);
			logger.info("Employee connected: " + session.getId());
			assignQueuedUser();
		} else if (message.equals("User requesting chat")) {
			session.getUserProperties().put("role", "user");
			USERS.add(session);
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
			EMPLOYEES.remove(session);
			Session assignedUser = EMPLOYEE_TO_USER_MAP.get(session);
			logger.info("assigned User: " + assignedUser);
			if (assignedUser != null) {
				if (assignedUser.isOpen()) {
					sendMessageToUser(assignedUser, "Employee disconnected. Please start a new chat!");
//					queueUser(assignedUser);
				}
			}
		} else {
			USERS.remove(session);
			for (Session emp : EMPLOYEE_TO_USER_MAP.keySet()) {
				Session user = EMPLOYEE_TO_USER_MAP.get(emp);
				if (user.equals(session)) {
					sendMessageToEmployee(emp, "User disconnected.");
					EMPLOYEE_TO_USER_MAP.remove(emp);
				}
			}
			dequeueUser(session);

		}
	}

	private boolean isEmployee(Session session) {
		return "employee".equals(session.getUserProperties().get("role"));
	}

	private void queueUser(Session userSession) {
		List<String> userQueue = CacheUtil.get(USER_QUEUE_KEY, new TypeReference<List<String>>() {
		});
		if (userQueue == null) {
			userQueue = new CopyOnWriteArrayList<>();
		}
		userQueue.add(userSession.getId());
		CacheUtil.save(USER_QUEUE_KEY, userQueue);
		assignQueuedUser();
	}

	private void dequeueUser(Session userSession) {
		List<String> userQueue = CacheUtil.get(USER_QUEUE_KEY, new TypeReference<List<String>>() {
		});
		if (userQueue != null) {
			userQueue.remove(userSession.getId());
			CacheUtil.save(USER_QUEUE_KEY, userQueue);
		}
	}

	private void assignQueuedUser() {
		List<String> userQueue = CacheUtil.get(USER_QUEUE_KEY, new TypeReference<List<String>>() {
		});
		if (userQueue == null || userQueue.isEmpty()) {
			return;
		}

		for (Session employeeSession : EMPLOYEES) {
			if (!userQueue.isEmpty() && EMPLOYEE_TO_USER_MAP.get(employeeSession) == null) {
				String userId = userQueue.remove(0);
				Session userSession = USERS.stream().filter(session -> session.getId().equals(userId)).findFirst()
						.orElse(null);

				if (userSession != null) {
					EMPLOYEE_TO_USER_MAP.put(employeeSession, userSession);
					sendMessageToUser(userSession, "You are connected with an employee.");
					sendMessageToEmployee(employeeSession, "New user assigned: " + userSession.getId());
				}
			}

			CacheUtil.save(USER_QUEUE_KEY, userQueue);
			if (userQueue.isEmpty()) {
				break;
			}
		}
	}

	private void sendMessageToAssignedUser(Session employeeSession, String message) {
		Session assignedUser = EMPLOYEE_TO_USER_MAP.get(employeeSession);
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
		for (Map.Entry<Session, Session> entry : EMPLOYEE_TO_USER_MAP.entrySet()) {
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