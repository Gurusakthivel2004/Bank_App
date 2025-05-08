package crm;

import java.util.HashMap;
import java.util.Map;

import enums.Constants.AccountsFields;
import model.Org;
import util.JsonUtils;
import util.OAuthConfig;

public class AccountsService {

    private CRMHttpService httpService = new CRMHttpService();

    public String pushAccount(Org org) throws Exception {
        Map<AccountsFields, Object> data = new HashMap<>();
        data.put(AccountsFields.ACCOUNT_NAME, org.getName());
        data.put(AccountsFields.INDUSTRY, org.getOrgType());
        data.put(AccountsFields.EMPLOYEES, org.getEmployees());
        data.put(AccountsFields.PHONE, org.getPhone().toString());

        String response = httpService.postToCrm(OAuthConfig.get("crm.account.endpoint"), data);

        return JsonUtils.getValueByPath(response, "data[0].details", "id");
    }
    
}