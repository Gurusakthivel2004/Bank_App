package initializer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dao.DaoFactory;
import enums.Constants.HttpStatusCodes;
import io.github.cdimascio.dotenv.Dotenv;
import pool.DBConnectionPool;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import schedular.CRMSchedular;
import schedular.ExpiredSessionSchedular;
import schedular.FixedDepositSchedular;
import schedular.PasswordUpdateScheduler;
import util.CustomException;
import util.Helper;

@WebListener
public class Initializer implements ServletContextListener {

	private static final Logger logger = LogManager.getLogger(Initializer.class);

	public static DBConnectionPool dbConnectionPool;
	private static JedisPool jedisPool;
	private static Dotenv dotenv = Helper.loadDotEnv();

	private static String URL;
	private static String USER;
	private static String PASSWORD;

	// REDIS Configuration
	private static final String REDIS_HOST = dotenv.get("REDIS_HOST");
	private static final int REDIS_PORT = Integer.parseInt(dotenv.get("REDIS_PORT"));
	private static final int REDIS_MAX_TOTAL = 20;
	private static final int REDIS_MAX_IDLE = 10;
	private static final int REDIS_MIN_IDLE = 1;
	private static final boolean REDIS_BLOCK_WHEN_EXHAUSTED = true;
	private static final int REDIS_CONNECTION_TIMEOUT = 2000;

	// Schedulers
	private static final PasswordUpdateScheduler PASSWORD_UPDATE_SCHEDULER = new PasswordUpdateScheduler();
	private static final ExpiredSessionSchedular EXPIRED_SESSION_SCHEDULAR = new ExpiredSessionSchedular();
	private static final FixedDepositSchedular FIXED_DEPOSIT_SCHEDULAR = new FixedDepositSchedular();
	private static final CRMSchedular CRM_SCHEDULAR = new CRMSchedular();
	private static final DatabaseInitializer DATABASE_INITIALIZER = new DatabaseInitializer();

	public static void setDataSource() throws SQLException, ClassNotFoundException {
		loadMySQLDriver();
		dbConnectionPool = new DBConnectionPool(URL, USER, PASSWORD);
		GenericObjectPoolConfig<Jedis> poolConfig = new GenericObjectPoolConfig<>();
		poolConfig.setMaxTotal(REDIS_MAX_TOTAL);
		poolConfig.setMaxIdle(REDIS_MAX_IDLE);
		poolConfig.setMinIdle(REDIS_MIN_IDLE);
		poolConfig.setBlockWhenExhausted(REDIS_BLOCK_WHEN_EXHAUSTED);

		jedisPool = new JedisPool(poolConfig, REDIS_HOST, REDIS_PORT, REDIS_CONNECTION_TIMEOUT);
	}

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		try {
			logger.info("Initializing Database Connection Pool...");

			createDB();

			dbConnectionPool = new DBConnectionPool(URL, USER, PASSWORD);
			logger.info("Custom Connection Pool reinitialized (with DB).");

			AdminCreator.createAdmin();

			logger.info("Initializing Redis...");
			GenericObjectPoolConfig<Jedis> poolConfig = new GenericObjectPoolConfig<>();
			poolConfig.setMaxTotal(REDIS_MAX_TOTAL);
			poolConfig.setMaxIdle(REDIS_MAX_IDLE);
			poolConfig.setMinIdle(REDIS_MIN_IDLE);
			poolConfig.setBlockWhenExhausted(REDIS_BLOCK_WHEN_EXHAUSTED);

			jedisPool = new JedisPool(poolConfig, REDIS_HOST, REDIS_PORT, REDIS_CONNECTION_TIMEOUT);
			logger.info("Redis initialized with connection pooling.");

			EXPIRED_SESSION_SCHEDULAR.startScheduler();
			PASSWORD_UPDATE_SCHEDULER.startScheduler();
			FIXED_DEPOSIT_SCHEDULAR.startScheduler();
			CRM_SCHEDULAR.startScheduler();

		} catch (Exception e) {
			logger.error("Error initializing resources: {}", e);
			throw new RuntimeException("Error initializing resources: ", e);
		}
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		try {
			if (dbConnectionPool != null) {
				dbConnectionPool.closePool();
				logger.info("Custom Connection Pool closed.");
			}
		} catch (Exception e) {
			logger.error("Error while closing Custom Connection Pool: {}", e.getMessage(), e);
		}

		try {
			if (jedisPool != null) {
				jedisPool.close();
				logger.info("Redis connection pool closed.");
			}
		} catch (Exception e) {
			logger.error("Error while closing Redis connection pool: {}", e.getMessage(), e);
		}

		try {
			if (PASSWORD_UPDATE_SCHEDULER != null) {
				PASSWORD_UPDATE_SCHEDULER.stopScheduler();
			}
			if (EXPIRED_SESSION_SCHEDULAR != null) {
				EXPIRED_SESSION_SCHEDULAR.stopScheduler();
			}
			if (CRM_SCHEDULAR != null) {
				CRM_SCHEDULAR.stopScheduler();
			}
			if (FIXED_DEPOSIT_SCHEDULAR != null) {
				FIXED_DEPOSIT_SCHEDULAR.stopScheduler();
			}
		} catch (Exception e) {
			logger.error("Error while stopping scheduler: {}", e.getMessage(), e);
		}
	}

	public static DBConnectionPool getDataSource() {
		return dbConnectionPool;
	}

	public static JedisPool getJedisPool() {
		return jedisPool;
	}

	private void createDB() throws Exception {
		try {
			switch (DaoFactory.CURRENT_DB) {
			case MYSQL:
				loadMySQLDriver();
				break;
			case POSTGRESQL:
				loadPostgreSQLDriver();
				return;
			default:
				throw new ClassNotFoundException("Unsupported database type: " + DaoFactory.CURRENT_DB);
			}
		} catch (ClassNotFoundException e) {
			logger.error("Database JDBC Driver not found: {}", e.getMessage());
			throw new CustomException("JDBC driver load failed for " + DaoFactory.CURRENT_DB,
					HttpStatusCodes.INTERNAL_SERVER_ERROR);
		}

		try {
			String tempUrl = URL.replaceFirst("/bank$", "");
			dbConnectionPool = new DBConnectionPool(tempUrl, USER, PASSWORD);
			logger.info("Custom Connection Pool initialized.");
			boolean dbExists = DATABASE_INITIALIZER.createDatabase(dbConnectionPool.getConnection());
			if (!dbExists) {
				dbConnectionPool.closePool();
				dbConnectionPool = new DBConnectionPool(URL, USER, PASSWORD);
				DATABASE_INITIALIZER.generateTablesFromXML(dbConnectionPool.getConnection());
			}
		} catch (SQLException e) {
			logger.error("Failed to initialize Database Connection Pool: {}", e.getMessage());
			throw new CustomException("Database Connection Pool initialization failed",
					HttpStatusCodes.INTERNAL_SERVER_ERROR);
		} finally {
			dbConnectionPool.closePool();
		}
	}

	private static void loadMySQLDriver() throws ClassNotFoundException {
		URL = dotenv.get("MYSQL_URL");
		USER = dotenv.get("MYSQL_USER");
		PASSWORD = dotenv.get("MYSQL_PASSWORD");
		Class.forName("com.mysql.cj.jdbc.Driver");
		logger.info("Loaded MySQL JDBC Driver.");
	}

	private void loadPostgreSQLDriver() throws SQLException, ClassNotFoundException {
		URL = dotenv.get("POSTGRESQL_URL");
		USER = dotenv.get("POSTGRESQL_USER");
		PASSWORD = dotenv.get("POSTGRESQL_PASSWORD");
		Class.forName("org.postgresql.Driver");
		logger.info("Loaded PostgreSQL JDBC Driver.");

		try (Connection conn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/postgres", "root", "root");
				Statement stmt = conn.createStatement()) {
			stmt.execute("DO $$ BEGIN IF NOT EXISTS (SELECT FROM pg_database WHERE datname = 'bank') "
					+ "THEN CREATE DATABASE bank; END IF; END $$;");
		}
	}

}