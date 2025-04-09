package model;

import java.math.BigDecimal;

public class FixedDeposit extends MarkedClass {

	private long id;
	private long accountNumber;
	private BigDecimal amount;
	private BigDecimal interestRate;
	private long startDate;
	private long maturityDate;
	private boolean isActive;
	private boolean isClosed;

	public long getId() {
		return id;
	}

	public FixedDeposit setId(long id) {
		this.id = id;
		return this;
	}

	public long getAccountNumber() {
		return accountNumber;
	}

	public FixedDeposit setAccountNumber(long accountNumber) {
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

	public long getStartDate() {
		return startDate;
	}

	public FixedDeposit setStartDate(long startDate) {
		this.startDate = startDate;
		return this;
	}

	public long getMaturityDate() {
		return maturityDate;
	}

	public FixedDeposit setMaturityDate(long maturityDate) {
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
