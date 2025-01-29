package service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import Enum.Constants.HttpStatusCodes;
import Enum.Constants.MessageStatus;
import dao.DAO;
import dao.MessageDAO;
import model.ColumnCriteria;
import model.Message;
import util.CustomException;
import util.Helper;
import util.ValidationUtil;

public class MessageService {

	private final Logger logger = LogManager.getLogger(MessageService.class);

	private final DAO<Message> messageDao = new MessageDAO();

	public Map<String, Object> getMessageDetails(Map<String, Object> msgMap) throws CustomException {
		logger.info("Fetching message details..");
		Map<String, Object> messageResult = new HashMap<>();

		System.out.println(msgMap.keySet());
		System.out.println(msgMap.values());

		List<Message> messages = messageDao.get(msgMap);
		Long logCount = messageDao.getDataCount(msgMap);

		messageResult.put("messages", messages);
		messageResult.put("count", logCount);

		logger.info("Retrieved message details..");
		return messageResult;
	}

	public void createMessage(Map<String, Object> messageMap) throws CustomException {
		logger.info("Creating a new message with data: {}", messageMap);
		messageMap.put("createdAt", System.currentTimeMillis());
		messageMap.put("messageStatus", MessageStatus.Pending);
		long senderId = Long.parseLong((String) messageMap.get("senderId"));
		ValidationUtil.userExists(senderId);

		Message message = Helper.createPojoFromMap(messageMap, Message.class);
		long messageId = messageDao.create(message);
		logger.info("Message successfully created with id: {}", messageId);

	}

	public void updateMessage(Map<String, Object> messageMap) throws CustomException {
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

		messageDao.update(columnCriteria, messageMap);

	}

}
