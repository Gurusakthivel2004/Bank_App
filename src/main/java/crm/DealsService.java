package crm;

import java.util.HashMap;
import java.util.Map;

import cache.CacheUtil;
import enums.Constants.DealsFields;
import util.JsonUtils;
import util.OAuthConfig;

public class DealsService {

	private CRMHttpService crmHttpService = new CRMHttpService();
	public static final String CRM_MODULE = "Deals";
	public static final String CRM_MODULE_PK = "Module Record Id";
	
	private DealsService() {}

	private static class SingletonHelper {
		private static final DealsService INSTANCE = new DealsService();
	}

	public static DealsService getInstance() {
		return SingletonHelper.INSTANCE;
	}

	public String pushDeal(String dealName, String amount, String accountName, String moduleRecordId) throws Exception {

		String jsonResponse = crmHttpService.fetchRecord(OAuthConfig.get("crm.contact.endpoint"), "Account_Name",
				accountName);

		String accountId = JsonUtils.getValueByPath(jsonResponse, "data[0].Account_Name", "id");
		String contactId = JsonUtils.getValueByPath(jsonResponse, "data[0]", "id");

		Map<DealsFields, Object> data = new HashMap<>();
		data.put(DealsFields.FK_ACCOUNT_NAME, accountId);
		data.put(DealsFields.FK_Contact_NAME, contactId);
		data.put(DealsFields.MODULE_RECORD_ID, moduleRecordId);
		data.put(DealsFields.AMOUNT, amount);
		data.put(DealsFields.STAGE, "Needs Analysis");
		data.put(DealsFields.TYPE, "New Business");
		data.put(DealsFields.DEAL_NAME, dealName);

		String response = crmHttpService.postToCrm(OAuthConfig.get("crm.deal.endpoint"), data);
		String recordId = JsonUtils.getValueByPath(response, "data[0].details", "id");
		CacheUtil.saveCRMRecordId(CRM_MODULE, moduleRecordId, recordId);
		
		return recordId;
	}
}