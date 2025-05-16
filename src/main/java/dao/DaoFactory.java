package dao;

public class DaoFactory {

	public enum DatabaseType {
		MYSQL, POSTGRESQL
	}

	public static final DatabaseType CURRENT_DB = DatabaseType.MYSQL;

	public static <T> DAO<T> getDAO(Class<T> clazz) {
		switch (CURRENT_DB) {
		case MYSQL:
		case POSTGRESQL:
			return getGenericDAO(clazz);
		default:
			throw new UnsupportedOperationException("Database type not supported!");
		}
	}

	@SuppressWarnings("unchecked")
	private static <T> DAO<T> getGenericDAO(Class<T> clazz) {
		switch (clazz.getSimpleName()) {
		case "Account":
			return (DAO<T>) AccountDAO.getInstance();
		case "ActivityLog":
			return (DAO<T>) ActivityLogDAO.getInstance();
		case "Branch":
			return (DAO<T>) BranchDAO.getInstance();
		case "Message":
			return (DAO<T>) MessageDAO.getInstance();
		case "Transaction":
			return (DAO<T>) TransactionDAO.getInstance();
		case "User":
		case "Staff":
		case "CustomerDetail":
			return (DAO<T>) UserDAO.getInstance();
		case "OauthProvider":
			return (DAO<T>) OauthProviderDAO.getInstance();
		case "UserSession":
			return (DAO<T>) UserSessionDAO.getInstance();
		case "OtpVerifications":
			return (DAO<T>) OtpVerificationsDAO.getInstance();
		case "Loan":
			return (DAO<T>) LoanDAO.getInstance();
		case "ModuleLog":
			return (DAO<T>) ModuleLogDAO.getInstance();
		case "FixedDeposit":
			return (DAO<T>) FixedDepositDAO.getInstance();
		case "OauthClientConfig":
			return (DAO<T>) OauthClientConfigDAO.getInstance();
		case "Org":
			return (DAO<T>) OrgDAO.getInstance();
		case "SubOrg":
			return (DAO<T>) SubOrgDAO.getInstance();
		case "OrgMember":
			return (DAO<T>) OrgMemberDAO.getInstance();
		case "SubOrgMember":
			return (DAO<T>) SubOrgMemberDAO.getInstance();
		case "FailedRequest":
			return (DAO<T>) FailedRequestDAO.getInstance();
		default:
			throw new UnsupportedOperationException("No DAO found for the provided class: " + clazz.getName());
		}
	}

}