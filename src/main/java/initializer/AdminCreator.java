package initializer;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.postgresql.util.PSQLException;

import dao.DaoFactory;
import enums.Constants.HttpStatusCodes;
import util.CustomException;
import util.Helper;

public class AdminCreator {

	private static final Logger logger = LogManager.getLogger(AdminCreator.class);

	private static final Map<DaoFactory.DatabaseType, String> INSERT_USER_MAP = new HashMap<>();
	private static final Map<DaoFactory.DatabaseType, String> INSERT_BRANCH_MAP = new HashMap<>();
	private static final Map<DaoFactory.DatabaseType, String> INSERT_STAFF_MAP = new HashMap<>();
	private static final Map<DaoFactory.DatabaseType, String> INSERT_ACCOUNT_MAP = new HashMap<>();

	static {
		// MySQL & PostgreSQL Compatible Queries
		INSERT_USER_MAP.put(DaoFactory.DatabaseType.MYSQL,
				"INSERT INTO user (fullname, email, phone, role, username, password, status, created_at, modified_at, performed_by, password_version, country_code) "
						+ "VALUES ('admin', 'admin@gmail.com', '9724851945', 'Manager', 'admin', '%s', 'Active', %d, %d, NULL, 1, 'IN');");

		INSERT_USER_MAP.put(DaoFactory.DatabaseType.POSTGRESQL,
				"INSERT INTO \"user\" (fullname, email, phone, role, username, password, status, created_at, modified_at, performed_by, password_version, country_code) "
						+ "VALUES ('admin', 'admin@gmail.com', '9724851945', 'Manager', 'admin', '%s', 'Active', %d, %d, NULL, 1, 'IN') RETURNING id;");

		INSERT_BRANCH_MAP.put(DaoFactory.DatabaseType.MYSQL,
				"INSERT INTO branch (ifsc_code, contact_number, name, address, created_at, modified_at, performed_by) "
						+ "VALUES ('HOR-124253', '9724851945', 'default', 'default address', %d, %d, %d);");

		INSERT_BRANCH_MAP.put(DaoFactory.DatabaseType.POSTGRESQL,
				"INSERT INTO branch (ifsc_code, contact_number, name, address, created_at, modified_at, performed_by) "
						+ "VALUES ('HOR-124253', '9724851945', 'default', 'default address', %d, %d, %d) RETURNING id;");

		INSERT_STAFF_MAP.put(DaoFactory.DatabaseType.MYSQL, "INSERT INTO staff (user_id, branch_id) VALUES (%d, %d);");
		INSERT_STAFF_MAP.put(DaoFactory.DatabaseType.POSTGRESQL,
				"INSERT INTO staff (user_id, branch_id) VALUES (%d, %d);");

		INSERT_ACCOUNT_MAP.put(DaoFactory.DatabaseType.MYSQL,
				"INSERT INTO account (account_number, branch_id, user_id, account_type, status, balance, min_balance, created_at, modified_at, performed_by, is_primary) "
						+ "VALUES ('1111', %d, %d, 'Operational', 'Active', 1000, 500, %d, %d, %d, 1);");

		INSERT_ACCOUNT_MAP.put(DaoFactory.DatabaseType.POSTGRESQL,
				"INSERT INTO account (account_number, branch_id, user_id, account_type, status, balance, min_balance, created_at, modified_at, performed_by, is_primary) "
						+ "VALUES ('1111', %d, %d, 'Operational', 'Active', 1000, 500, %d, %d, %d, true);");
	}

	public static void createAdmin() throws CustomException {
		DaoFactory.DatabaseType dbType = DaoFactory.CURRENT_DB;

		if (!INSERT_USER_MAP.containsKey(dbType)) {
			throw new CustomException("Unsupported database type: " + dbType, HttpStatusCodes.INTERNAL_SERVER_ERROR);
		}

		try (Connection connection = Initializer.getDataSource().getConnection();
				Statement statement = connection.createStatement()) {

			logger.info("Creating admin...");

			long currentTime = System.currentTimeMillis();
			String hashedPassword = Helper.hashPassword("default", 1);
			String userQuery = String.format(INSERT_USER_MAP.get(dbType), hashedPassword, currentTime, currentTime);

			logger.info("Executing query: " + userQuery);

			int adminId;
			if (dbType == DaoFactory.DatabaseType.POSTGRESQL) {
				try (ResultSet generatedKeys = statement.executeQuery(userQuery)) {
					if (generatedKeys.next()) {
						adminId = generatedKeys.getInt(1);
						logger.info("Admin created with ID: " + adminId);
					} else {
						throw new SQLException("Failed to retrieve admin ID.");
					}
				}
			} else {
				statement.executeUpdate(userQuery, Statement.RETURN_GENERATED_KEYS);
				try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
					if (generatedKeys.next()) {
						adminId = generatedKeys.getInt(1);
						logger.info("Admin created with ID: " + adminId);
					} else {
						throw new SQLException("Failed to retrieve admin ID.");
					}
				}
			}

			// Insert branch
			String branchQuery = String.format(INSERT_BRANCH_MAP.get(dbType), currentTime, currentTime, adminId);
			logger.info("Executing query: " + branchQuery);

			int branchId;
			if (dbType == DaoFactory.DatabaseType.POSTGRESQL) {
				try (ResultSet generatedKeys = statement.executeQuery(branchQuery)) {
					if (generatedKeys.next()) {
						branchId = generatedKeys.getInt(1);
						logger.info("Branch created with ID: " + branchId);
					} else {
						throw new SQLException("Failed to retrieve branch ID.");
					}
				}
			} else {
				statement.executeUpdate(branchQuery, Statement.RETURN_GENERATED_KEYS);
				try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
					if (generatedKeys.next()) {
						branchId = generatedKeys.getInt(1);
						logger.info("Branch created with ID: " + branchId);
					} else {
						throw new SQLException("Failed to retrieve branch ID.");
					}
				}
			}

			// Insert staff
			String staffQuery = String.format(INSERT_STAFF_MAP.get(dbType), adminId, branchId);
			logger.info("Executing query: " + staffQuery);
			statement.execute(staffQuery);

			// Insert account
			String accountQuery = String.format(INSERT_ACCOUNT_MAP.get(dbType), branchId, adminId, currentTime,
					currentTime, adminId);
			logger.info("Executing query: " + accountQuery);
			statement.execute(accountQuery);

			logger.info("Initial records created successfully.");

		} catch (SQLIntegrityConstraintViolationException e) {
			logger.warn("Admin already exists: ", e);
		} catch (PSQLException e) {
			if (e.getSQLState().equals("23505")) {
				logger.warn("Admin already exists: ", e);
			} else {
				logger.error("Database error: ", e);
			}
		} catch (SQLException e) {
			Helper.handleSQLException(e);
		} catch (Exception e) {
			logger.error("Admin creation failed: {}", e.getMessage(), e);
			throw new CustomException("Admin creation failed", HttpStatusCodes.INTERNAL_SERVER_ERROR);
		}
	}
}
