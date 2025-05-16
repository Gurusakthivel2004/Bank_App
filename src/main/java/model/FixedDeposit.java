package model;

import java.math.BigDecimal;

public class FixedDeposit extends MarkedClass {

	private Long id;
	private Long accountNumber;
	private BigDecimal amount;
	private BigDecimal interestRate;
	private Long startDate;
	private Long maturityDate;
	private boolean isActive;
	private boolean isClosed;

	public Long getId() {
		return id;
	}

	public FixedDeposit setId(Long id) {
		this.id = id;
		return this;
	}

	public Long getAccountNumber() {
		return accountNumber;
	}

	public FixedDeposit setAccountNumber(Long accountNumber) {
		this.accountNumber = accountNumber;
		return this;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public FixedDeposit setAmount(BigDecimal amount) {
		this.amount = amount;
		return this;
	}

	public BigDecimal getInterestRate() {
		return interestRate;
	}

	public FixedDeposit setInterestRate(BigDecimal interestRate) {
		this.interestRate = interestRate;
		return this;
	}

	public Long getStartDate() {
		return startDate;
	}

	public FixedDeposit setStartDate(Long startDate) {
		this.startDate = startDate;
		return this;
	}

	public Long getMaturityDate() {
		return maturityDate;
	}

	public FixedDeposit setMaturityDate(Long maturityDate) {
		this.maturityDate = maturityDate;
		return this;
	}

	public boolean isActive() {
		return isActive;
	}

	public FixedDeposit setActive(boolean isActive) {
		this.isActive = isActive;
		return this;
	}

	public boolean isClosed() {
		return isClosed;
	}

	public FixedDeposit setClosed(boolean isClosed) {
		this.isClosed = isClosed;
		return this;
	}

	@Override
	public String toString() {
		return "FixedDeposit [id=" + id + ", accountNumber=" + accountNumber + ", amount=" + amount + ", interestRate="
				+ interestRate + ", startDate=" + startDate + ", maturityDate=" + maturityDate + ", isActive="
				+ isActive + ", isClosed=" + isClosed + "]";
	}
}
