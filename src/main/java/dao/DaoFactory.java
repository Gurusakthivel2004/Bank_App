package dao;

public class DaoFactory {

	public enum DatabaseType {
		MYSQL
	}

	private static final DatabaseType CURRENT_DB = DatabaseType.MYSQL;

	public static <T> DAO<T> getDAO(Class<T> clazz) {
		switch (CURRENT_DB) {
		case MYSQL:
			return getMysqlDAO(clazz);
		default:
			throw new UnsupportedOperationException("Database type not supported!");
		}
	}

	@SuppressWarnings("unchecked")

	private static <T> DAO<T> getMysqlDAO(Class<T> clazz) {
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
		default:
			throw new UnsupportedOperationException("No DAO found for the provided class: " + clazz.getName());
		}
	}

}