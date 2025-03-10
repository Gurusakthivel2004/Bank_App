package model;

public class Staff extends User {

	private Long userId;
	private Long branchId;

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

	@Override
	public String toString() {
		return "Staff [userID=" + userId + ", branchID=" + branchId;
	}
}
