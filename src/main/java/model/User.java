package model;

import enums.Constants.Role;
import enums.Constants.Status;

public class User extends MarkedClass {

	private Long id;
	private String fullname;
	private String email;
	private String phone;
	private Role role;
	private String username;
	private String password;
	private String countryCode;
	private Integer passwordVersion;
	private Status status;
	private Long createdAt;
	private Long modifiedAt;
	private Long performedBy;

	public User() {}

	public Long getId() {
		return id;
	}

	public User setId(Long id) {
		this.id = id;
		return this;
	}

	public String getFullname() {
		return fullname;
	}

	public User setFullname(String fullname) {
		this.fullname = fullname;
		return this;
	}

	public String getEmail() {
		return email;
	}

	public User setEmail(String email) {
		this.email = email;
		return this;
	}

	public String getPhone() {
		return phone;
	}

	public User setPhone(String phone) {
		this.phone = phone;
		return this;
	}

	public String getUsername() {
		return username;
	}

	public User setUsername(String username) {
		this.username = username;
		return this;
	}

	public String getPassword() {
		return password;
	}

	public User setPassword(String password) {
		this.password = password;
		return this;
	}

	public String getRole() {
		return role != null ? role.name() : null;
	}

	public User setRole(String role) {
		if (role != null && !role.trim().isEmpty()) {
			this.role = Role.valueOf(role.toUpperCase());
		} else {
			this.role = null;
		}
		return this;
	}

	public Role getRoleEnum() {
		return this.role;
	}

	public User setRoleEnum(Role role) {
		this.role = role;
		return this;
	}

	public String getStatus() {
		return status != null ? status.name() : null;
	}

	public User setStatus(String status) {
		if (status != null && !status.trim().isEmpty()) {
			this.status = Status.valueOf(status.toUpperCase());
		} else {
			this.status = null;
		}
		return this;
	}

	public Status getStatusEnum() {
		return this.status;
	}

	public User setStatusEnum(Status status) {
		this.status = status;
		return this;
	}

	public Long getCreatedAt() {
		return createdAt;
	}

	public User setCreatedAt(Long millis) {
		this.createdAt = millis;
		return this;
	}

	public Long getModifiedAt() {
		return modifiedAt;
	}

	public User setModifiedAt(Long millis) {
		this.modifiedAt = millis;
		return this;
	}

	public Long getPerformedBy() {
		return performedBy;
	}

	public User setPerformedBy(Long performedBy) {
		this.performedBy = performedBy;
		return this;
	}

	public Integer getPasswordVersion() {
		return passwordVersion;
	}

	public void setPasswordVersion(Integer passwordVersion) {
		this.passwordVersion = passwordVersion;
	}

	public String getCountryCode() {
		return countryCode;
	}

	public User setCountryCode(String countryCode) {
		this.countryCode = countryCode;
		return this;
	}
	
	@Override
	public String toString() {
		return "User [id=" + id + ", fullname=" + fullname + ", email=" + email + ", phone=" + phone + ", role=" + role
				+ ", username=" + username + ", password=" + password + ", status=" + status + ", createdAt="
				+ createdAt + ", modifiedAt=" + modifiedAt + ", performedBy=" + performedBy + "]";
	}

}
