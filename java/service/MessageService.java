package service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dao.DAO;
import dao.DaoFactory;
import enums.Constants.HttpStatusCodes;
import enums.Constants.MessageStatus;
import model.ColumnCriteria;
import model.Message;
import util.CustomException;
import util.Helper;
import util.ValidationUtil;

public class MessageService {

	private static Logger logger = LogManager.getLogger(MessageService.class);
	private DAO<Message> messagDAO = DaoFactory.getDAO(Message.class);

	private MessageService() {
	}

	private static class SingletonHelper {
		private static final MessageService INSTANCE = new MessageService();
	}

	public static MessageService getInstance() {
		return SingletonHelper.INSTANCE;
	}

	public Map<String, Object> getMessageDetails(Map<String, Object> msgMap) throws Exception {
		logger.info("Fetching message details..");
		Map<String, Object> messageResult = new HashMap<>();

		List<Message> messages = messagDAO.get(msgMap);
		long logCount = messagDAO.getDataCount(msgMap);

		messageResult.put("messages", messages);
		messageResult.put("count", logCount);

		logger.info("Retrieved message details..");
		return messageResult;
	}

	public void createMessage(Map<String, Object> messageMap) throws Exception {
		logger.info("Creating a new message with data: {}", messageMap);
		messageMap.put("createdAt", System.currentTimeMillis());
		messageMap.put("messageStatus", MessageStatus.Pending);

		Message message = Helper.createPojoFromMap(messageMap, Message.class);
		long senderId = message.getSenderId();
		ValidationUtil.userExists(senderId);
		long messageId = messagDAO.create(message);
		logger.info("Message successfully created with id: {}", messageId);

	}

	public void updateMessage(Map<String, Object> messageMap) throws Exception {
		logger.info("Attempting to update message details.");

		if (messageMap == null || messageMap.size() == 0 || !messageMap.containsKey("messageId")) {
			throw new CustomException("Please enter fields to update", HttpStatusCodes.BAD_REQUEST);
		}

		Object id = messageMap.remove("messageId");
		ValidationUtil.validateUpdateFields(messageMap, Message.class);

		List<String> fields = new ArrayList<>(Arrays.asList("modifiedAt"));
		List<Object> values = new ArrayList<>(Arrays.asList(System.currentTimeMillis()));

		for (String key : messageMap.keySet()) {
			Object value = messageMap.get(key);

			fields.add(key);
			values.add(value);
		}

		messageMap = new HashMap<>();
		messageMap.put("messageId", id);

		ColumnCriteria columnCriteria = new ColumnCriteria().setFields(fields).setValues(values);

		messagDAO.update(columnCriteria, messageMap);

	}

}
