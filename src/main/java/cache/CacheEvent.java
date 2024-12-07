package cache;

public class CacheEvent {
	private final String entityType;

	public CacheEvent(String entityType) {
		this.entityType = entityType;
	}

	public String getEntityType() {
		return entityType;
	}
}
