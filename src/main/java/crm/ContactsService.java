package crm;

import java.util.HashMap;
import java.util.Map;

import cache.CacheUtil;
import enums.Constants.ContactsFields;
import model.Org;
import model.User;
import util.JsonUtils;
import util.OAuthConfig;
import util.PhoneUtil;
import util.UsernameValidator;

public class ContactsService {

	private CRMHttpService crmHttpService = CRMHttpService.getInstance();
	public static final String CRM_MODULE = "Contacts";
	public static final String CRM_MODULE_PK = "Email";
	private static final String CONTACT_ENDPOINT = OAuthConfig.get("crm.contacts.endpoint");

	private ContactsService() {
	}

	private static class SingletonHelper {
		private static final ContactsService INSTANCE = new ContactsService();
	}

	public static ContactsService getInstance() {
		return SingletonHelper.INSTANCE;
	}

	public String insertIfAbsent(Org org, User user, String accountId) throws Exception {
		// Fetch contacts
		String contactsJsonResponse = crmHttpService.fetchRecord(CONTACT_ENDPOINT, "Account_Name", org.getName());

		String contactId = JsonUtils.getValueByPath(contactsJsonResponse, "data[0]", "id");
		// Push contacts record
		if (contactId == null) {
			contactId = pushContact(user, accountId);
		}

		return contactId;
	}

	public String pushContact(User user, String accountId) throws Exception {

		String formattedPhone = PhoneUtil.formatByRegionCode(user.getPhone(), user.getCountryCode());

		String username = user.getFullname() + user.getUsername();
		Integer score = UsernameValidator.validateUsername(username);
		String status = classifyUsernameScore(score);
		
		

		Map<ContactsFields, Object> data = new HashMap<>();
		data.put(ContactsFields.FK_ACCOUNT_NAME, accountId);
		data.put(ContactsFields.EMAIL, user.getEmail());
		data.put(ContactsFields.FIRST_NAME, user.getFullname());
		data.put(ContactsFields.LAST_NAME, user.getUsername());
		data.put(ContactsFields.PHONE, formattedPhone);
		data.put(ContactsFields.USERNAME_STATUS, status);
		data.put(ContactsFields.EMAIL_STATUS, user.getEmailStatus() ? "verified" : "not verified");

		String response = crmHttpService.postToCrm(CONTACT_ENDPOINT, data);
		String recordId = JsonUtils.getValueByPath(response, "data[0].details", "id");
		CacheUtil.saveCRMRecordId(CRM_MODULE, user.getEmail(), recordId);

		return recordId;
	}

	public String updateRecord(String updateJson) throws Exception {
		String contactsJsonResponse = crmHttpService.putToCrm(CONTACT_ENDPOINT, updateJson);
		return contactsJsonResponse;
	}

	private String classifyUsernameScore(Integer score) {
		if (score < 3) {
			return "likely_spam";
		} else if (score < 7) {
			return "uncertain";
		} else {
			return "genuine";
		}
	}

}
