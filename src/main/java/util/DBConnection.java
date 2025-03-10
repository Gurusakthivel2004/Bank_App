package util;

import java.sql.Connection;
import java.sql.SQLException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.zaxxer.hikari.HikariDataSource;
import servlet.Initializer;

public class DBConnection {

	private static final Logger logger = LogManager.getLogger(DBConnection.class);
	private static DBConnection instance;
	private static HikariDataSource dataSource;

	private DBConnection() {
		dataSource = Initializer.getDataSource();
	}

	public static DBConnection getInstance() {
		if (instance == null) {
			synchronized (DBConnection.class) {
				if (instance == null) {
					instance = new DBConnection();
				}
			}
		}
		return instance;
	}

	public Connection getConnection() throws SQLException {
		try {
			return dataSource.getConnection();
		} catch (Exception e) {
			logger.error("Failed to get DB connection: {}", e.getMessage());
			throw e;
		}
	}
}