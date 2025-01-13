package model;

public class Staff extends User {

	private Long userId;
	private Long branchId;
	private Long createdAt;
	private Long modifiedAt;
	private Long performedBy;

	public Long getUserId() {
		return userId;
	}

	public Staff setUserId(Long userId) {
		this.userId = userId;
		return this;
	}

	public Long getBranchId() {
		return branchId;
	}

	public Staff setBranchId(Long branchId) {
		this.branchId = branchId;
		return this;
	}

	public Long getCreatedAt() {
		return createdAt;
	}

	public Staff setCreatedAt(Long createdAt) {
		this.createdAt = createdAt;
		return this;
	}

	public Long getModifiedAt() {
		return modifiedAt;
	}

	public Staff setModifiedAt(Long modifiedAt) {
		this.modifiedAt = modifiedAt;
		return this;
	}

	public Long getPerformedBy() {
		return performedBy;
	}

	public Staff setPerformedBy(Long performedBy) {
		this.performedBy = performedBy;
		return this;
	}

	@Override
	public String toString() {
		return "Staff [userID=" + userId + ", branchID=" + branchId + ", createdAt=" + createdAt + ", modifiedAt="
				+ modifiedAt + ", performedBy=" + performedBy + "]";
	}
}
