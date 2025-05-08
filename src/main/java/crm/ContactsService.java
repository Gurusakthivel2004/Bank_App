package crm;

import java.util.HashMap;
import java.util.Map;

import enums.Constants.ContactsFields;
import model.User;
import util.OAuthConfig;

public class ContactsService {

    private CRMHttpService crmHttpService = new CRMHttpService();

    public void pushContact(User user, String accountId) throws Exception {
        Map<ContactsFields, Object> data = new HashMap<>();
        data.put(ContactsFields.FK_ACCOUNT_NAME, accountId);
        data.put(ContactsFields.EMAIL, user.getEmail());
        data.put(ContactsFields.FIRST_NAME, user.getFullname());
        data.put(ContactsFields.LAST_NAME, user.getUsername());
        data.put(ContactsFields.PHONE, user.getPhone().toString());

        crmHttpService.postToCrm(OAuthConfig.get("crm.contact.endpoint"), data);
    }
}
