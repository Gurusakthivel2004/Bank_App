package crm;

import java.util.HashMap;
import java.util.Map;

import cache.CacheUtil;
import enums.Constants.AccountsFields;
import model.Org;
import util.JsonUtils;
import util.OAuthConfig;

public class AccountsService {

	private static CRMHttpService httpService = new CRMHttpService();
	public static final String CRM_MODULE = "Accounts";
	public static final String CRM_MODULE_PK = "Phone";
	
	private AccountsService() {}

	private static class SingletonHelper {
		private static final AccountsService INSTANCE = new AccountsService();
	}

	public static AccountsService getInstance() {
		return SingletonHelper.INSTANCE;
	}
	
	public String pushAccount(Org org) throws Exception {
		String phone = org.getPhone().toString();
		
		Map<AccountsFields, Object> data = new HashMap<>();
		data.put(AccountsFields.ACCOUNT_NAME, org.getName());
		data.put(AccountsFields.INDUSTRY, org.getOrgType());
		data.put(AccountsFields.EMPLOYEES, org.getEmployees());
		data.put(AccountsFields.PHONE, phone);

		String response = httpService.postToCrm(OAuthConfig.get("crm.account.endpoint"), data);
		String recordId = JsonUtils.getValueByPath(response, "data[0].details", "id");
		CacheUtil.saveCRMRecordId(CRM_MODULE, phone, recordId);

		return recordId;
	}

}