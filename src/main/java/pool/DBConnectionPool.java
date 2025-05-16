package pool;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DBConnectionPool {
	private static final Logger logger = LogManager.getLogger(DBConnectionPool.class);

	private static final int MAX_POOL_SIZE = 10;
	private static final int MIN_IDLE = 1;
	private static final long IDLE_TIMEOUT = 30_000;
	private static final long MAX_LIFETIME = 1_80_00_000;
	private static final long CONNECTION_TIMEOUT = 10_000;

	private final String url;
	private final String user;
	private final String password;
	private final BlockingQueue<PooledConnection> pool;
	private final ScheduledExecutorService cleaner;

	public DBConnectionPool(String url, String user, String password) throws SQLException {
		this.url = url;
		this.user = user;
		this.password = password;
		this.pool = new LinkedBlockingQueue<>(MAX_POOL_SIZE);
		this.cleaner = Executors.newScheduledThreadPool(1);

		for (int i = 0; i < MIN_IDLE; i++) {
			pool.offer(new PooledConnection(createNewConnection(), this));
		}

		cleaner.scheduleAtFixedRate(this::cleanup, IDLE_TIMEOUT, IDLE_TIMEOUT, TimeUnit.MILLISECONDS);
	}

	private Connection createNewConnection() throws SQLException {
		return DriverManager.getConnection(url, user, password);
	}

	public Connection getConnection() throws InterruptedException, SQLException {
		PooledConnection pooledConn = pool.poll(CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS);

		if (pooledConn == null || pooledConn.isExpired() || !pooledConn.isValid()) {
			if (pooledConn != null) {
				pooledConn.close();
			}
			synchronized (this) {
				if (pool.size() < MAX_POOL_SIZE) {
					pooledConn = new PooledConnection(createNewConnection(), this);
					return pooledConn.getConnection();
				}
			}

			throw new SQLException("Connection timeout: No available connections");
		}

		return pooledConn.getConnection();
	}

	public void returnToPool(PooledConnection pooledConn) {
		if (pooledConn != null && pooledConn.isValid()) {
			pool.offer(pooledConn);
		} else {
			try {
				pooledConn.close();
			} catch (SQLException ignored) {
			}
		}
	}

	private void cleanup() {
		pool.removeIf(conn -> {
			if (conn.isExpired() || !conn.isValid()) {
				try {
					conn.close();
				} catch (SQLException e) {
					logger.error("Failed to close connection", e);
				}
				return true;
			}
			return false;
		});
	}

	public void closePool() throws SQLException {
		for (PooledConnection conn : pool) {
			conn.close();
		}
		cleaner.shutdown();
	}

	private static class PooledConnection implements InvocationHandler {
		private final Connection realConnection;
		private final DBConnectionPool pool;
		private final long creationTime;
		private final Connection proxyConnection;

		public PooledConnection(Connection conn, DBConnectionPool pool) {
			this.realConnection = conn;
			this.pool = pool;
			this.creationTime = System.currentTimeMillis();
			this.proxyConnection = (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
					new Class[] { Connection.class }, this);
		}

		public Connection getConnection() {
			return proxyConnection;
		}

		public boolean isExpired() {
			return (System.currentTimeMillis() - creationTime) > MAX_LIFETIME;
		}

		public boolean isValid() {
			try {
				return !realConnection.isClosed() && realConnection.isValid(2);
			} catch (SQLException e) {
				return false;
			}
		}

		public void close() throws SQLException {
			realConnection.close();
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			if ("close".equals(method.getName())) {
				pool.returnToPool(this);
				return null;
			}
			return method.invoke(realConnection, args);
		}
	}
}
