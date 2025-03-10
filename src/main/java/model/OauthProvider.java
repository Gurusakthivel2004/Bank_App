package model;

public class OauthProvider extends MarkedClass {

	private Long id;
	private Long userId;
	private String provider;
	private String providerUserId;
	private String accessToken;
	private String refreshToken;
	private Integer expiresIn;
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

	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	public String getProviderUserId() {
		return providerUserId;
	}

	public void setProviderUserId(String providerUserId) {
		this.providerUserId = providerUserId;
	}

	public String getAccessToken() {
		return accessToken;
	}

	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}

	public String getRefreshToken() {
		return refreshToken;
	}

	public void setRefreshToken(String refreshToken) {
		this.refreshToken = refreshToken;
	}

	public Integer getExpiresIn() {
		return expiresIn;
	}

	public void setExpiresIn(Integer expiresIn) {
		this.expiresIn = expiresIn;
	}

	public Long getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Long createdAt) {
		this.createdAt = createdAt;
	}

	@Override
	public String toString() {
		return "OauthProvider [id=" + id + ", userId=" + userId + ", provider=" + provider + ", providerUserId="
				+ providerUserId + ", accessToken=" + accessToken + ", refreshToken=" + refreshToken + ", expiresIn="
				+ expiresIn + ", createdAt=" + createdAt + "]";
	}

}
