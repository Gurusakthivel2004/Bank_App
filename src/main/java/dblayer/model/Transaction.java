package dblayer.model;

import java.math.BigDecimal;

public class Transaction extends MarkedClass {

	private Long id;
	private Long customerId;
	private Long accountNumber;
	private Long transactionAccountNumber;
	private String transactionType;
	private String status;
	private String remarks;
	private BigDecimal amount;
	private BigDecimal closingBalance;
	private String ifsc;
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
		return transactionType;
	}

	public void setTransactionType(String transactionType) {
		this.transactionType = transactionType;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
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

	public String getIfsc() {
		return ifsc;
	}

	public void setIfsc(String ifsc) {
		this.ifsc = ifsc;
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
				+ ", status=" + status + ", remarks=" + remarks + ", amount=" + amount + ", closingBalance="
				+ closingBalance + ", ifsc=" + ifsc + ", transactionIfsc=" + transactionIfsc + ", bankName=" + bankName
				+ ", transactionTime=" + transactionTime + ", performedBy=" + performedBy + "]";
	}
}
