package model;

public class OtpVerifications extends MarkedClass {

	private Long id;
	private Long userId;
	private Long accountNumber;
	private String otp;
	private Boolean isVerified;
	private Integer attempts;
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

	public String getOtp() {
		return otp;
	}

	public void setOtp(String otp) {
		this.otp = otp;
	}

	public Boolean getIsVerified() {
		return isVerified;
	}

	public void setIsVerified(Boolean isVerified) {
		this.isVerified = isVerified;
	}

	public Integer getAttempts() {
		return attempts;
	}

	public void setAttempts(Integer attempts) {
		this.attempts = attempts;
	}

	public Long getExpiresAt() {
		return expiresAt;
	}

	public void setExpiresAt(Long expiresAt) {
		this.expiresAt = expiresAt;
	}

	public Long getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Long createdAt) {
		this.createdAt = createdAt;
	}

	@Override
	public String toString() {
		return "OtpVerifications [id=" + id + ", userId=" + userId + ", otp=" + otp + ", isVerified=" + isVerified
				+ ", attempts=" + attempts + ", expiresAt=" + expiresAt + ", createdAt=" + createdAt + "]";
	}

	public Long getAccountNumber() {
		return accountNumber;
	}

	public void setAccountNumber(Long accountNumber) {
		this.accountNumber = accountNumber;
	}

}
