package service;

import java.lang.reflect.Method;
import java.util.Arrays;
import cache.CacheEvent;
import cache.CacheInvalidationListener;
import cache.CacheLayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import util.CustomException;
import util.Helper;

public class CacheService {

	private final Logger logger = LogManager.getLogger(CacheService.class);
	private final CacheLayer cache = new CacheLayer();
	private final CacheInvalidationListener listener = new CacheInvalidationListener(cache);

	public Object fetchData(Object daoInstance, String methodName, Object... parameters) throws CustomException {
		String cacheKey = Helper.generateKey(daoInstance.getClass().getSimpleName(), methodName, parameters);
		logger.info("Attempting to fetch data for cacheKey: {}", cacheKey);

		Object cachedData = cache.get(cacheKey);
		if (cachedData != null) {
			logger.info("Cache hit for key: {}", cacheKey);
			return cachedData;
		}

		logger.info("Cache miss for key: {}. Fetching from DB...", cacheKey);
		Object data = fetchFromDB(daoInstance, methodName, parameters);

		cache.put(cacheKey, data);
		logger.info("Data stored in cache for key: {}", cacheKey);

		return data;
	}
	// invalidation
	public void invalidateData(String entityType) {
		logger.info("Invalidating cache for entityType: {} ", entityType);
		listener.onEvent(new CacheEvent(entityType));
		logger.info("Cache invalidation event processed for entityType: {}", entityType);
	}

	@SuppressWarnings("unchecked")
	public <T> T fetchFromDB(Object daoInstance, String methodName, Object... parameters) throws CustomException {
		try {
			Class<?> daoClass = daoInstance.getClass();
			Method method = findMethod(daoClass, methodName, parameters);
			if (method == null) {
				logger.error("Method {} not found in class {} with parameters {}", methodName, daoClass.getName(),
						parameters);
				throw new CustomException("Error occurred while fetching data.");
			}
			logger.info("Method {} found in class {} with parameters {}", methodName, daoClass.getName(),
					parameters);
			logger.info("Invoking method {} on DAO instance {}", methodName, daoClass.getName());
			return (T) method.invoke(daoInstance, parameters);
		} catch (Exception e) {
			logger.error("Error during dynamic DAO method invocation", e);
			throw new CustomException("Error occurred while fetching data.");
		}
	}

	private Method findMethod(Class<?> clazz, String methodName, Object[] parameters) {
		for (Method method : clazz.getDeclaredMethods()) {
			System.out.println(method.getName() + " " + methodName);
			if (method.getName().equals(methodName) && matchParameterTypes(method.getParameterTypes(), parameters)) {
				return method;
			}
		}
		return null;
	}

	private boolean matchParameterTypes(Class<?>[] paramTypes, Object[] parameters) {
		if (paramTypes.length != parameters.length) {
			return false;
		}
		logger.info("Parameters: {}", Arrays.toString(parameters));
		logger.info("Parameter Types: {}",
				Arrays.stream(paramTypes).map(param -> param.getClass().getName()).toArray());

		for (int i = 0; i < paramTypes.length; i++) {
			if (parameters[i] != null && !paramTypes[i].isAssignableFrom(parameters[i].getClass())) {
				return false;
			}
		}
		return true;
	}
}
