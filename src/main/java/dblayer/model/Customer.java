package dblayer.model;

public class Customer extends User {
	
	private Long userId;
    private String panNumber;
    private Long aadharNumber;
    
	public Customer() {}


	public String getPanNumber() {
		return panNumber;
	}

	public void setPanNumber(String panNumber) {
		this.panNumber = panNumber;
	}

	public Long getAadharNumber() {
		return aadharNumber;
	}

	public void setAadharNumber(Long aadharNumber) {
		this.aadharNumber = aadharNumber;
	}

	@Override
	public String toString() {
		return super.toString() + "Customer [id=" + userId + ", panNumber=" + panNumber + ", aadharNumber=" + aadharNumber;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}
    
}
