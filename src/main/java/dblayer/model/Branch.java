package dblayer.model;

public class Branch extends MarkedClass {

	private Long id;
    private String ifscCode;
    private Long contactNumber;
    private String name;
    private String address;
    private Long createdAt;
    private Long modifiedAt;
    private Long performedBy;

	public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIfscCode() {
        return ifscCode;
    }

    public void setIfscCode(String ifscCode) {
        this.ifscCode = ifscCode;
    }

    public Long getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(Long contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
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
		return "Branch [id=" + id + ", ifscCode=" + ifscCode + ", contactNumber=" + contactNumber + ", name=" + name
				+ ", address=" + address + ", createdAt=" + createdAt + ", modifiedAt=" + modifiedAt + ", performedBy="
				+ performedBy + "]";
	}
}

