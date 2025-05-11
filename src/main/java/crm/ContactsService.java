package crm;

import java.util.HashMap;
import java.util.Map;

import cache.CacheUtil;
import enums.Constants.ContactsFields;
import model.User;
import util.JsonUtils;
import util.OAuthConfig;

public class ContactsService {

	private CRMHttpService crmHttpService = new CRMHttpService();
	public static final String CRM_MODULE = "Contacts";
	public static final String CRM_MODULE_PK = "Email";
	
	private ContactsService() {}

	private static class SingletonHelper {
		private static final ContactsService INSTANCE = new ContactsService();
	}

	public static ContactsService getInstance() {
		return SingletonHelper.INSTANCE;
	}

	public String pushContact(User user, String accountId) throws Exception {
		
		Map<ContactsFields, Object> data = new HashMap<>();
		data.put(ContactsFields.FK_ACCOUNT_NAME, accountId);
		data.put(ContactsFields.EMAIL, user.getEmail());
		data.put(ContactsFields.FIRST_NAME, user.getFullname());
		data.put(ContactsFields.LAST_NAME, user.getUsername());
		data.put(ContactsFields.PHONE, user.getPhone().toString());

		String response = crmHttpService.postToCrm(OAuthConfig.get("crm.contact.endpoint"), data);
		String recordId = JsonUtils.getValueByPath(response, "data[0].details", "id");
		CacheUtil.saveCRMRecordId(CRM_MODULE, user.getEmail(), recordId);
		
		return recordId;
	}
}
