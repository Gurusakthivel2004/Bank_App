package dao;

import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import model.ColumnCriteria;
import model.Criteria;
import model.OtpVerifications;
import util.Helper;
import util.SQLHelper;

public class OtpVerificationsDAO implements DAO<OtpVerifications> {

	private static Logger logger = LogManager.getLogger(OtpVerificationsDAO.class);

	private OtpVerificationsDAO() {}

	private static class SingletonHelper {
		private static final OtpVerificationsDAO INSTANCE = new OtpVerificationsDAO();
	}

	public static OtpVerificationsDAO getInstance() {
		return SingletonHelper.INSTANCE;
	}

	public long create(OtpVerifications otpVerifications) throws Exception {
		logger.info("Inserting otp info...");

		Helper.checkNullValues(otpVerifications);
		Object insertedValue = SQLHelper.insert(otpVerifications);

		return Helper.convertToLong(insertedValue);
	}

	public List<OtpVerifications> get(Map<String, Object> otpMap) throws Exception {
		Criteria criteria = DAOHelper.initializeCriteria(OtpVerifications.class);
		DAOHelper.applyOtpFilters(criteria, otpMap);
		return SQLHelper.get(criteria, OtpVerifications.class);
	}

	public void update(ColumnCriteria columnCriteria, Map<String, Object> otpMap) throws Exception {
		logger.info("Updating oauth provider info{}", otpMap);
		Criteria criteria = new Criteria().setClazz(OtpVerifications.class);
		DAOHelper.applyOtpFilters(criteria, otpMap);
		SQLHelper.update(columnCriteria, criteria);
	}

	public long getDataCount(Map<String, Object> messageMap) throws Exception {
		return 0;
	}

}