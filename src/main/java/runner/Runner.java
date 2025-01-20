package runner;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dao.DAOHelper;
import model.ActivityLog;
import model.Criteria;
import util.CustomException;
import util.SQLHelper;

public class Runner {
	public static void main(String[] args) throws CustomException, SQLException {
		Map<String, Object> logMap = new HashMap<>();
		logMap.put("branchId", 7l);
		List<String> joinTable = new ArrayList<>(Arrays.asList("account", "staff"));
		Criteria criteria = DAOHelper
				.buildJoinCriteria(ActivityLog.class, joinTable, new ArrayList<>(), new ArrayList<>(),
						new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), " JOIN ", true)
				.setSelectColumn(Collections.singletonList("activityLog.*"));
		DAOHelper.addJoinCondition(criteria, true, "account.account_number", "EQUAL_TO",
				"activityLog.user_account_number");
		DAOHelper.addJoinCondition(criteria, true, "staff.branch_id", "EQUAL_TO", "account.branch_id");
		DAOHelper.addConditionIfPresent(criteria, logMap, "branchId", "staff.branch_id", "EQUAL_TO", 0L);
		List<ActivityLog> result = SQLHelper.get(criteria, ActivityLog.class);
		System.out.println(result);
		
	}
}
