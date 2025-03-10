package servlet;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import enums.Constants.HttpStatusCodes;
import io.github.cdimascio.dotenv.Dotenv;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import util.CustomException;
import util.Helper;

@WebListener
public class Initializer implements ServletContextListener {

	private static final Logger logger = LogManager.getLogger(Initializer.class);

	private static HikariDataSource dataSource;
	private static JedisPool jedisPool;
	private static Dotenv dotenv = Helper.loadDotEnv();
	private static final String URL = dotenv.get("DB_URL");
	private static final String USER = dotenv.get("DB_USER");
	private static final String PASSWORD = dotenv.get("DB_PASSWORD");
	// JDBC
	private static final int MAX_POOL_SIZE = 10; // Maximum pool size
	private static final int MIN_IDLE = 1; // Minimum number of idle connection
	private static final int IDLE_TIMEOUT = 30000; // Close idle connections after 30s
	private static final int MAX_LIFETIME = 18000000; // Close connections older than 300 mins
	private static final int CONNECTION_TIMEOUT = 10000; // Wait 10s before timeout
	// REDIS
	private static final String REDIS_HOST = dotenv.get("REDIS_HOST");
	private static final int REDIS_PORT = Integer.parseInt(dotenv.get("REDIS_PORT"));
	private static final int REDIS_MAX_TOTAL = 20; // Maximum active connections
	private static final int REDIS_MAX_IDLE = 10; // Maximum idle connections
	private static final int REDIS_MIN_IDLE = 1; // Minimum idle connections
	private static final boolean REDIS_BLOCK_WHEN_EXHAUSTED = true; // Block when no connection
	private static final int REDIS_CONNECTION_TIMEOUT = 2000; // Connection timeout

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		try {
			// Initialize HikariCP
			logger.info("Initializing HikariCP...");
			HikariConfig config = new HikariConfig();
			config.setJdbcUrl(URL);
			config.setUsername(USER);
			config.setPassword(PASSWORD);
			config.setMaximumPoolSize(MAX_POOL_SIZE);
			config.setMinimumIdle(MIN_IDLE);
			config.setIdleTimeout(IDLE_TIMEOUT);
			config.setMaxLifetime(MAX_LIFETIME);
			config.setConnectionTimeout(CONNECTION_TIMEOUT);
			try {
				Class.forName("com.mysql.cj.jdbc.Driver");
			} catch (Exception e) {
				logger.error("Failed to load MySQL JDBC driver: {}", e.getMessage());
				throw new CustomException("MySQL JDBC driver load failed", HttpStatusCodes.INTERNAL_SERVER_ERROR);
			}
			dataSource = new HikariDataSource(config);
			logger.info("HikariCP initialized successfully.");

			// Initialize Redis
			logger.info("Initializing Redis...");
			GenericObjectPoolConfig<Jedis> poolConfig = new GenericObjectPoolConfig<>();
			poolConfig.setMaxTotal(REDIS_MAX_TOTAL);
			poolConfig.setMaxIdle(REDIS_MAX_IDLE);
			poolConfig.setMinIdle(REDIS_MIN_IDLE);
			poolConfig.setBlockWhenExhausted(REDIS_BLOCK_WHEN_EXHAUSTED);

			jedisPool = new JedisPool(poolConfig, REDIS_HOST, REDIS_PORT, REDIS_CONNECTION_TIMEOUT);
			logger.info("Redis initialized with connection pooling.");

		} catch (Exception e) {
			logger.error("Error initializing resources: {}", e);
			throw new RuntimeException("Error initializing resources: ", e);
		}
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		try {
			if (dataSource != null) {
				dataSource.close();
				logger.info("HikariCP closed.");
			}
		} catch (Exception e) {
			logger.error("Error while closing HikariCP: {}", e.getMessage(), e);
		}

		try {
			if (jedisPool != null) {
				jedisPool.close();
				logger.info("Redis connection pool closed.");
			}
		} catch (Exception e) {
			logger.error("Error while closing Redis connection pool: {}", e.getMessage(), e);
		}
	}

	public static HikariDataSource getDataSource() {
		return dataSource;
	}

	public static JedisPool getJedisPool() {
		return jedisPool;
	}
}