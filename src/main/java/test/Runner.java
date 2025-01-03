package test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import dblayer.model.Account;
import dblayer.model.Criteria;
import util.CustomException;
import util.Helper;
import util.SQLHelper;

public class Runner {
	public static void main(String[] args) {
		boolean joinCondition = true;
		Criteria staffJoinCriteria = Helper.buildJoinCriteria(Account.class, Arrays.asList("branch"), new ArrayList<>(),
				new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(),
				joinCondition);

		staffJoinCriteria.setSelectColumn(Arrays.asList("account.*", "branch.name"));
		// Dynamically add join conditions
		Helper.addJoinCondition(staffJoinCriteria, joinCondition, "account.branch_id", "=", "branch.id");
		Helper.addCondition(staffJoinCriteria, true, "user_id", "=", 34l);
		staffJoinCriteria.setAlias(Arrays.asList(null, "branch_name"));
		try {
			List<Object> accountJoin = SQLHelper.get(staffJoinCriteria);
			System.out.println(accountJoin);
		} catch (CustomException e) {
			System.out.print(e);
		}
	}
}
