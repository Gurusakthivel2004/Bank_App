package dao;

import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import model.ColumnCriteria;
import model.Criteria;
import model.ModuleLog;
import util.Helper;
import util.SQLHelper;

public class ModuleLogDAO implements DAO<ModuleLog> {

	private static Logger logger = LogManager.getLogger(ModuleLogDAO.class);

	private ModuleLogDAO() {}

	private static class SingletonHelper {
		private static final ModuleLogDAO INSTANCE = new ModuleLogDAO();
	}

	public static ModuleLogDAO getInstance() {
		return SingletonHelper.INSTANCE;
	}

	public long create(ModuleLog loanLog) throws Exception {
		logger.info("Inserting loan log info...");

		Helper.checkNullValues(loanLog);
		Object insertedValue = SQLHelper.insert(loanLog);

		return Helper.convertToLong(insertedValue);
	}

	public List<ModuleLog> get(Map<String, Object> moduleMap) throws Exception {
		Criteria criteria = DAOHelper.initializeCriteria(ModuleLog.class);
		DAOHelper.addConditionIfPresent(criteria, moduleMap, "moduleId", "module_id", "EQUAL_TO", 0l);
		return SQLHelper.get(criteria, ModuleLog.class);
	}

	public void update(ColumnCriteria columnCriteria, Map<String, Object> otpMap) throws Exception {

	}

	public long getDataCount(Map<String, Object> loanMap) throws Exception {
		return 0;
	}
}
