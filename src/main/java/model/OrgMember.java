package model;

import enums.Constants.Role;

public class OrgMember extends MarkedClass {
	private Long userId;
	private Long orgId;
	private Role userType;

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public Long getOrgId() {
		return orgId;
	}

	public void setOrgId(Long orgId) {
		this.orgId = orgId;
	}

	public String getUserType() {
		return userType != null ? userType.name() : null;
	}

	public void setUserType(String userType) {
		if (userType != null && !userType.trim().isEmpty()) {
			this.userType = Role.valueOf(userType.toUpperCase());
		} else {
			this.userType = null;
		}
	}

	public Role getUserTypeEnum() {
		return this.userType;
	}

	public void setUserTypeEnum(Role userType) {
		this.userType = userType;
	}

}
