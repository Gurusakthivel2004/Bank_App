package dao;

import java.math.BigInteger;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import Enum.Constants.HttpStatusCodes;
import model.ColumnCriteria;
import model.Criteria;
import model.Message;
import util.CustomException;
import util.Helper;
import util.SQLHelper;

public class MessageDAO implements DAO<Message> {

	private static final Logger logger = LogManager.getLogger(MessageDAO.class);

	public Long create(Message message) throws CustomException {
		logger.info("Inserting message details...");

		Helper.checkNullValues(message);
		Long logId;
		try {
			logId = ((BigInteger) SQLHelper.insert(message)).longValue();
		} catch (CustomException ce) {
			throw ce;
		} catch (Exception e) {
			logger.error("Error while inserting message", e);
			throw new CustomException("Failed to create message", HttpStatusCodes.INTERNAL_SERVER_ERROR);
		}

		logger.info("message created successfully with ID: " + logId);
		return logId;
	}

	public List<Message> get(Map<String, Object> messageMap) throws CustomException {

		Criteria criteria = DAOHelper.getMessageCriteria(messageMap);
		try {
			return SQLHelper.get(criteria, Message.class);
		} catch (CustomException e) {
			throw e;
		} catch (SQLException e) {
			logger.error("Error while fetching message details: ", e);
			throw new CustomException("Failed to fetch mesage details: ", HttpStatusCodes.INTERNAL_SERVER_ERROR);
		}
	}

	public Long getDataCount(Map<String, Object> messageMap) throws CustomException {
		try {
			Criteria criteria = DAOHelper.getMessageCriteria(messageMap);
			criteria.setOffsetValue(-1L).setAggregateFunction("COUNT").setAggregateOperator("*");
			Long count = SQLHelper.getCount(criteria, Message.class);
			if (count == 0) {
				throw new CustomException("Unexpected error occured while fetching message details",
						HttpStatusCodes.INTERNAL_SERVER_ERROR);
			}
			return count;
		} catch (SQLException e) {
			logger.error("Error while fetching message details: ", e);
			throw new CustomException("Failed to fetch message details: ", HttpStatusCodes.INTERNAL_SERVER_ERROR);
		}
	}

	public void update(ColumnCriteria columnCriteria, Map<String, Object> messageMap) throws CustomException {
		try {
			Criteria criteria = new Criteria().setClazz(Message.class);
			DAOHelper.addConditionIfPresent(criteria, messageMap, "messageId", "id", "EQUAL_TO", 0l);
			SQLHelper.update(columnCriteria, criteria);
		} catch (CustomException e) {
			throw e;
		} catch (Exception e) {
			logger.error("Error updating message.", e);
			throw new CustomException("Failed to update message", HttpStatusCodes.INTERNAL_SERVER_ERROR);
		}
	}

}
