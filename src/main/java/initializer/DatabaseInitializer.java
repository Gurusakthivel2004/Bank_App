package initializer;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import dao.DaoFactory;
import dao.DaoFactory.DatabaseType;
import enums.Constants.HttpStatusCodes;
import util.CustomException;

public class DatabaseInitializer {
	private static final Logger logger = LogManager.getLogger(DatabaseInitializer.class);
	private static final String SCHEMA_FILE = getSchemaFile();

	public void generateTablesFromDump() throws CustomException {
		try (Connection connection = Initializer.getDataSource().getConnection();
				Statement statement = connection.createStatement()) {

			logger.info("Initializing database using schema: {}", SCHEMA_FILE);

			String sql = loadSchemaSQL();
			if (sql.isEmpty()) {
				logger.error("Schema SQL file is empty or not found!");
				throw new CustomException("Schema file is empty", HttpStatusCodes.INTERNAL_SERVER_ERROR);
			}

			String[] queries = sql.split(";");
			for (String query : queries) {
				executeQuery(connection, query);
			}

			logger.info("Database schema executed successfully.");

		} catch (Exception e) {
			logger.error("Database initialization failed: {}", e.getMessage(), e);
			throw new CustomException("Database initialization failed", HttpStatusCodes.INTERNAL_SERVER_ERROR);
		}
	}

	public void generateTablesFromXML(Connection connection) {
		try {
			InputStream inputStream = getClass().getClassLoader().getResourceAsStream("database-schema.xml");

			if (inputStream == null) {
				throw new FileNotFoundException("File not found in resources folder");
			}

			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document document = builder.parse(inputStream);
			document.getDocumentElement().normalize();

			NodeList tables = document.getElementsByTagName("table");

			for (int i = 0; i < tables.getLength(); i++) {
				Element tableElement = (Element) tables.item(i);
				String tableName = tableElement.getAttribute("name");

				StringBuilder sql = new StringBuilder("CREATE TABLE " + tableName + " (\n");
				StringBuilder primaryKey = new StringBuilder();
				StringBuilder foreignKeys = new StringBuilder();

				NodeList columns = tableElement.getElementsByTagName("column");

				for (int j = 0; j < columns.getLength(); j++) {
					Element col = (Element) columns.item(j);
					String name = col.getAttribute("name");
					String type = col.getAttribute("type");
					boolean autoIncrement = "true".equals(col.getAttribute("auto_increment"));
					boolean primaryKeyFlag = "true".equals(col.getAttribute("primary_key"));
					boolean unique = "true".equals(col.getAttribute("unique"));
					boolean notNull = "true".equals(col.getAttribute("not_null"));
					String defaultValue = col.getAttribute("default");
					String foreignKey = col.getAttribute("foreign_key");
					String onDelete = col.getAttribute("on_delete");

					if (type.equals("enum")) {
						String values = col.getAttribute("values");
						type = "enum('" + values.replaceAll(",", "','") + "')";
					}

					sql.append("  ").append(name).append(" ").append(type);

					if (autoIncrement)
						sql.append(" AUTO_INCREMENT");
					if (notNull)
						sql.append(" NOT NULL");
					if (unique)
						sql.append(" UNIQUE");
					if (!defaultValue.isEmpty())
						sql.append(" DEFAULT ").append(defaultValue);

					sql.append(",\n");

					if (primaryKeyFlag) {
						if (primaryKey.length() != 0)
							primaryKey.append(", ");
						primaryKey.append(name);
					}

					if (foreignKey.length() != 0) {
						foreignKeys.append("  CONSTRAINT fk_").append(tableName).append("_").append(name)
								.append(" FOREIGN KEY (").append(name).append(") REFERENCES ")
								.append(foreignKey.replace("(", " ("))
								.append(onDelete.isEmpty() ? "" : " ON DELETE " + onDelete).append(",\n");
					}

				}

				if (primaryKey.length() != 0) {
					sql.append("  PRIMARY KEY (").append(primaryKey).append("),\n");
				}
				if (foreignKeys.length() > 0) {
					sql.append(foreignKeys);
				}

				sql.setLength(sql.length() - 2);
				sql.append("\n);");

				executeQuery(connection, sql.toString());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void executeQuery(Connection connection, String query) throws CustomException {
		try (Statement statement = connection.createStatement()) {

			query = query.trim();
			if (!query.isEmpty()) {
				try {
					statement.execute(query);
				} catch (SQLException e) {
					logger.error("Failed to execute query: {}\nError: {}", query, e.getMessage(), e);
					throw new RuntimeException(e);
				}
			}
			logger.info("Database schema executed successfully.");

		} catch (Exception e) {
			logger.error("Database initialization failed: {}", e.getMessage(), e);
			throw new CustomException("Database initialization failed", HttpStatusCodes.INTERNAL_SERVER_ERROR);
		}
	}

	public boolean createDatabase(Connection connection) throws CustomException {
		try (Statement statement = connection.createStatement()) {

			String createQuery = "CREATE DATABASE bankapp;";
			String checkDBQuery = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = 'bankapp';";
			ResultSet rs = statement.executeQuery(checkDBQuery);
			while (rs.next()) {
				long count = rs.getLong("COUNT(*)");
				if (count > 0) {
					return true;
				}
			}
			statement.execute(createQuery);
			logger.info("Database schema executed successfully.");
			return false;
		} catch (Exception e) {
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

	private static String getSchemaFile() {
		if (DaoFactory.CURRENT_DB == DatabaseType.MYSQL) {
			return "/mysql.sql";
		} else if (DaoFactory.CURRENT_DB == DatabaseType.POSTGRESQL) {
			return "/postgres_dump.sql";
		} else {
			throw new UnsupportedOperationException("Unsupported database type: " + DaoFactory.CURRENT_DB);
		}
	}
}
