package dao;

import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import model.ColumnCriteria;
import model.Criteria;
import model.Message;
import util.Helper;
import util.SQLHelper;

public class MessageDAO implements DAO<Message> {

	private static Logger logger = LogManager.getLogger(MessageDAO.class);

	private MessageDAO() {
	}

	private static class SingletonHelper {
		private static final MessageDAO INSTANCE = new MessageDAO();
	}

	public static MessageDAO getInstance() {
		return SingletonHelper.INSTANCE;
	}

	public long create(Message message) throws Exception {
		logger.info("Inserting message details...");

		Helper.checkNullValues(message);
		Object insertedValue = SQLHelper.insert(message);
		return Helper.convertToLong(insertedValue);
	}

	public List<Message> get(Map<String, Object> messageMap) throws Exception {

		Criteria criteria = DAOHelper.getMessageCriteria(messageMap);
		return SQLHelper.get(criteria, Message.class);
	}

	public long getDataCount(Map<String, Object> messageMap) throws Exception {
		Criteria criteria = DAOHelper.getMessageCriteria(messageMap);
		criteria.setOffsetValue(-1L).setAggregateFunction("COUNT").setAggregateOperator("*");
		Long count = SQLHelper.getCount(criteria, Message.class);
		return count;
	}

	public void update(ColumnCriteria columnCriteria, Map<String, Object> messageMap) throws Exception {

		Criteria criteria = new Criteria().setClazz(Message.class);
		DAOHelper.addConditionIfPresent(criteria, messageMap, "messageId", "id", "EQUAL_TO", 0l);
		SQLHelper.update(columnCriteria, criteria);

	}

}
