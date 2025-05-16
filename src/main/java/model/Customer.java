package model;

public class Customer extends User {
	
	private Long userId;
    private String panNumber;
    private Long aadharNumber;
    
	public Customer() {}


	public String getPanNumber() {
		return panNumber;
	}

	public Customer setPanNumber(String panNumber) {
		this.panNumber = panNumber;
		return this;
	}

	public Long getAadharNumber() {
		return aadharNumber;
	}

	public Customer setAadharNumber(Long aadharNumber) {
		this.aadharNumber = aadharNumber;
		return this;
	}

	@Override
	public String toString() {
		return super.toString() + "Customer [id=" + userId + ", panNumber=" + panNumber + ", aadharNumber=" + aadharNumber;
	}

	public Long getUserId() {
		return userId;
	}

	public Customer setUserId(Long userId) {
		this.userId = userId;
		return this;
	}
    
}
