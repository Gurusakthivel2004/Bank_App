package initializer;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import enums.Constants.HttpStatusCodes;
import util.CustomException;

public class DatabaseInitializer {
	private static final Logger logger = LogManager.getLogger(DatabaseInitializer.class);
	private static final String SCHEMA_FILE = "/schema.sql";

	public void initializeDatabase() throws CustomException {
		try (Connection connection = Initializer.getDataSource().getConnection();
				Statement statement = connection.createStatement()) {

			logger.info("Initializing database...");

			String sql = loadSchemaSQL();
			if (sql.isEmpty()) {
				logger.error("Schema SQL file is empty or not found!");
				throw new CustomException("Schema file is empty", HttpStatusCodes.INTERNAL_SERVER_ERROR);
			}

			String[] queries = sql.split(";");
			for (String query : queries) {
				query = query.trim();
				if (!query.isEmpty()) {
					try {
						statement.execute(query);
					} catch (SQLException e) {
						logger.error("Failed to execute query: {}\nError: {}", query, e.getMessage(), e);
						throw new RuntimeException(e);
					}
				}
			}

			logger.info("Database schema executed successfully.");

		} catch (SQLException e) {
			logger.error("Database initialization failed: {}", e.getMessage(), e);
			throw new CustomException("Database initialization failed", HttpStatusCodes.INTERNAL_SERVER_ERROR);
		}
	}

	private static String loadSchemaSQL() {
		try (InputStream inputStream = DatabaseInitializer.class.getResourceAsStream(SCHEMA_FILE)) {
			if (inputStream == null) {
				logger.error("Schema file not found: {}", SCHEMA_FILE);
				return "";
			}

			try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
				return reader.lines().collect(Collectors.joining("\n"));
			}

		} catch (Exception e) {
			logger.error("Failed to load schema.sql: {}", e.getMessage(), e);
			return "";
		}
	}
}
