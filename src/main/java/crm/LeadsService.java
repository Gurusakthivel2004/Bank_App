package crm;

import java.util.HashMap;
import java.util.Map;

import cache.CacheUtil;
import enums.Constants.LeadsFields;
import model.SubOrg;
import util.JsonUtils;
import util.OAuthConfig;

public class LeadsService {

	private CRMHttpService crmHttpService = CRMHttpService.getInstance();
	public static final String CRM_MODULE = "Leads";
	public static final String CRM_MODULE_PK = "Email";
	private static final String LEAD_ENDPOINT = OAuthConfig.get("crm.leads.endpoint");
	
	private LeadsService() {}

	private static class SingletonHelper {
		private static final LeadsService INSTANCE = new LeadsService();
	}

	public static LeadsService getInstance() {
		return SingletonHelper.INSTANCE;
	}
	
	public String updateRecord(String updateJson) throws Exception {
		String leadsJsonResponse = crmHttpService.putToCrm(LEAD_ENDPOINT, updateJson);
		return leadsJsonResponse;
	}

	public String pushLead(SubOrg subOrg, String company, String email) throws Exception {
		
		Map<LeadsFields, Object> data = new HashMap<>();
		data.put(LeadsFields.FIRST_NAME, subOrg.getName());
		data.put(LeadsFields.LAST_NAME, "test");
		data.put(LeadsFields.COMPANY, company);
		data.put(LeadsFields.EMAIL, email);

		String response = crmHttpService.postToCrm(LEAD_ENDPOINT, data);
		String recordId = JsonUtils.getValueByPath(response, "data[0].details", "id");
		CacheUtil.saveCRMRecordId(CRM_MODULE, email, recordId);
		
		return recordId;
	}
}
