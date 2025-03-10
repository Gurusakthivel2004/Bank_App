package model;

public class CustomerDetail extends Customer {

	private Long customerId;
	private String dob;
	private String fatherName;
	private String motherName;
	private String address;
	private String maritalStatus;

	public CustomerDetail() {
	}

	public String getDob() {
		return dob;
	}

	public CustomerDetail setDob(String dob) {
		this.dob = dob;
		return this;
	}

	public String getFatherName() {
		return fatherName;
	}

	public CustomerDetail setFatherName(String fatherName) {
		this.fatherName = fatherName;
		return this;
	}

	public String getMotherName() {
		return motherName;
	}

	public CustomerDetail setMotherName(String motherName) {
		this.motherName = motherName;
		return this;
	}

	public String getAddress() {
		return address;
	}

	public CustomerDetail setAddress(String address) {
		this.address = address;
		return this;
	}

	public String getMaritalStatus() {
		return maritalStatus;
	}

	public CustomerDetail setMaritalStatus(String maritalStatus) {
		this.maritalStatus = maritalStatus;
		return this;
	}

	@Override
	public String toString() {
		return super.toString() + "CustomerDetail [id=" + getId() + ", dob=" + dob + ", fatherName=" + fatherName
				+ ", motherName=" + motherName + ", address=" + address + ", maritalStatus=" + maritalStatus + "]";
	}

	public Long getCustomerId() {
		return customerId;
	}

	public CustomerDetail setCustomerId(Long customerId) {
		this.customerId = customerId;
		return this;
	}

}