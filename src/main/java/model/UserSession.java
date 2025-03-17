package model;

public class UserSession extends MarkedClass {

	private Long id;
	private Long userId;
	private String sessionId;
	private Long providerId;
	private Long expiresAt;
	private Long createdAt;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public Long getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Long createdAt) {
		this.createdAt = createdAt;
	}

	public Long getProviderId() {
		return providerId;
	}

	public void setProviderId(Long providerId) {
		this.providerId = providerId;
	}

	public Long getExpiresAt() {
		return expiresAt;
	}

	public void setExpiresAt(Long expiresAt) {
		this.expiresAt = expiresAt;
	}

	@Override
	public String toString() {
		return "UserSession [id=" + id + ", userId=" + userId + ", sessionId=" + sessionId + ", providerId="
				+ providerId + ", expiresAt=" + expiresAt + ", createdAt=" + createdAt + "]";
	}

}
