package dblayer.model;

public class Staff extends User {
	
	private Long userId;
    private Long branchId;
	private Long createdAt;
    private Long modifiedAt;
    private Long performedBy;
    
    public Long getUserId() {
		return userId;
	}
    
	public void setUserId(Long userId) {
		this.userId = userId;
	}
	
	public Long getBranchId() {
		return branchId;
	}
	
	public void setBranchId(Long branchId) {
		this.branchId = branchId;
	}
	
	public Long getCreatedAt() {
		return createdAt;
	}
	
	public void setCreatedAt(Long createdAt) {
		this.createdAt = createdAt;
	}
	
	public Long getModifiedAt() {
		return modifiedAt;
	}
	
	public void setModifiedAt(Long modifiedAt) {
		this.modifiedAt = modifiedAt;
	}
	
	public Long getPerformedBy() {
		return performedBy;
	}
	
	public void setPerformedBy(Long performedBy) {
		this.performedBy = performedBy;
	}
    
	@Override
	public String toString() {
		return "Staff [userID=" + userId + ", branchID=" + branchId + ", createdAt=" + createdAt + ", modifiedAt="
				+ modifiedAt + ", performedBy=" + performedBy + "]";
	}
}
