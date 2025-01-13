package model;

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

	public Branch setId(Long id) {
		this.id = id;
		return this;
	}

	public String getIfscCode() {
		return ifscCode;
	}

	public Branch setIfscCode(String ifscCode) {
		this.ifscCode = ifscCode;
		return this;
	}

	public Long getContactNumber() {
		return contactNumber;
	}

	public Branch setContactNumber(Long contactNumber) {
		this.contactNumber = contactNumber;
		return this;
	}

	public String getName() {
		return name;
	}

	public Branch setName(String name) {
		this.name = name;
		return this;
	}

	public String getAddress() {
		return address;
	}

	public Branch setAddress(String address) {
		this.address = address;
		return this;
	}

	public Long getCreatedAt() {
		return createdAt;
	}

	public Branch setCreatedAt(Long createdAt) {
		this.createdAt = createdAt;
		return this;
	}

	public Long getModifiedAt() {
		return modifiedAt;
	}

	public Branch setModifiedAt(Long modifiedAt) {
		this.modifiedAt = modifiedAt;
		return this;
	}

	public Long getPerformedBy() {
		return performedBy;
	}

	public Branch setPerformedBy(Long performedBy) {
		this.performedBy = performedBy;
		return this;
	}

	@Override
	public String toString() {
		return "Branch [id=" + id + ", ifscCode=" + ifscCode + ", contactNumber=" + contactNumber + ", name=" + name
				+ ", address=" + address + ", createdAt=" + createdAt + ", modifiedAt=" + modifiedAt + ", performedBy="
				+ performedBy + "]";
	}
}
