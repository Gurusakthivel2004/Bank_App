package crm;

import java.util.HashMap;
import java.util.Map;

import enums.Constants.DealsFields;
import util.JsonUtils;
import util.OAuthConfig;

public class DealsService {

	private CRMHttpService crmHttpService = new CRMHttpService();

	public void pushDeal(String dealName, String amount, String accountName, String moduleRecordId) throws Exception {

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

		crmHttpService.postToCrm(OAuthConfig.get("crm.deal.endpoint"), data);
	}
}