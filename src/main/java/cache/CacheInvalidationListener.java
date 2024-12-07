package cache;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import service.CacheService;

public class CacheInvalidationListener {
	private final CacheLayer cache;
    private final Logger logger = LogManager.getLogger(CacheService.class);

	public CacheInvalidationListener(CacheLayer cache) {
		this.cache = cache;
	}

	public void onEvent(CacheEvent event) {
		String entityType = event.getEntityType();
		cache.invalidate(entityType);
		logger.info("Cache invalidated for prefix: " + entityType);
	}
}
