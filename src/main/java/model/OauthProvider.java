package model;

public class OauthProvider extends MarkedClass {

	private Long id;
	private Long userId;
	private String providerUserId;
	private Long clientConfigId;
	private String org;
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

	public Long getOauthClientId() {
		return clientConfigId;
	}

	public void setOauthClientId(Long oauthClientId) {
		this.clientConfigId = oauthClientId;
	}

	public String getProviderUserId() {
		return providerUserId;
	}

	public void setProviderUserId(String providerUserId) {
		this.providerUserId = providerUserId;
	}

	public String getOrg() {
		return org;
	}

	public void setOrg(String org) {
		this.org = org;
	}

	@Override
	public String toString() {
		return "OauthProvider [id=" + id + ", userId=" + userId + ", providerUserId=" + providerUserId
				+ ", clientConfigId=" + clientConfigId + ", org=" + org + ", accessToken=" + accessToken
				+ ", refreshToken=" + refreshToken + ", expiresIn=" + expiresIn + ", createdAt=" + createdAt + "]";
	}

}
