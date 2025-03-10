package model;

import java.math.BigDecimal;

import enums.Constants.TransactionStatus;
import enums.Constants.TransactionType;

public class Transaction extends MarkedClass {

	private Long id;
	private Long customerId;
	private Long accountNumber;
	private Long transactionAccountNumber;
	private TransactionType transactionType;
	private TransactionStatus transactionStatus;
	private String remarks;
	private BigDecimal amount;
	private BigDecimal closingBalance;
	private String transactionIfsc;
	private String bankName;
	private Long transactionTime;
	private Long performedBy;

	public Transaction() {
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getCustomerId() {
		return customerId;
	}

	public void setCustomerId(Long customerId) {
		this.customerId = customerId;
	}

	public Long getAccountNumber() {
		return accountNumber;
	}

	public void setAccountNumber(Long accountNumber) {
		this.accountNumber = accountNumber;
	}

	public Long getTransactionAccountNumber() {
		return transactionAccountNumber;
	}

	public void setTransactionAccountNumber(Long transactionAccountNumber) {
		this.transactionAccountNumber = transactionAccountNumber;
	}

	public String getTransactionType() {
		return transactionType != null ? transactionType.name() : null;
	}

	public Transaction setTransactionType(String transactionType) {
		if (transactionType != null && !transactionType.trim().isEmpty()) {
			this.transactionType = TransactionType.valueOf(transactionType.toUpperCase());
		} else {
			this.transactionType = null;
		}
		return this;
	}

	public TransactionType getTransactionTypeEnum() {
		return this.transactionType;
	}

	public Transaction setTransactionTypeEnum(TransactionType transactionType) {
		this.transactionType = transactionType;
		return this;
	}

	public String getTransactionStatus() {
		return transactionStatus != null ? transactionStatus.name() : null;
	}

	public Transaction setTransactionStatus(String transactionStatus) {
		if (transactionStatus != null && !transactionStatus.trim().isEmpty()) {
			this.transactionStatus = TransactionStatus.valueOf(transactionStatus.toUpperCase());
		} else {
			this.transactionStatus = null;
		}
		return this;
	}

	public TransactionStatus getTransactionStatusEnum() {
		return this.transactionStatus;
	}

	public Transaction setTransactionStatusEnum(TransactionStatus transactionStatus) {
		this.transactionStatus = transactionStatus;
		return this;
	}

	public String getRemarks() {
		return remarks;
	}

	public void setRemarks(String remarks) {
		this.remarks = remarks;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}

	public BigDecimal getClosingBalance() {
		return closingBalance;
	}

	public void setClosingBalance(BigDecimal closingBalance) {
		this.closingBalance = closingBalance;
	}

	public Long getTransactionTime() {
		return transactionTime;
	}

	public void setTransactionTime(Long transactionTime) {
		this.transactionTime = transactionTime;
	}

	public Long getPerformedBy() {
		return performedBy;
	}

	public void setPerformedBy(Long performedBy) {
		this.performedBy = performedBy;
	}

	public String getBankName() {
		return bankName;
	}

	public void setBankName(String bankName) {
		this.bankName = bankName;
	}

	public String getTransactionIfsc() {
		return transactionIfsc;
	}

	public void setTransactionIfsc(String transactionIfsc) {
		this.transactionIfsc = transactionIfsc;
	}

	@Override
	public String toString() {
		return "Transaction [id=" + id + ", customerId=" + customerId + ", accountNumber=" + accountNumber
				+ ", transactionAccountNumber=" + transactionAccountNumber + ", transactionType=" + transactionType
				+ ", status=" + transactionStatus + ", remarks=" + remarks + ", amount=" + amount + ", closingBalance="
				+ closingBalance + ", transactionIfsc=" + transactionIfsc + ", bankName=" + bankName
				+ ", transactionTime=" + transactionTime + ", performedBy=" + performedBy + "]";
	}
}
