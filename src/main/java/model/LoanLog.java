package model;

import enums.Constants.LoanAction;

public class LoanLog extends MarkedClass {

    private Long id;
    private LoanAction action;
    private Long loanId;
    private Long accountNumber;
    private String message;
    private Long createdAt;
    private Long performedBy;

    public Long getId() {
        return id;
    }

    public LoanLog setId(Long id) {
        this.id = id;
        return this;
    }

    public String getAction() {
        return action.toString();
    }
    
    public LoanAction getActionasEnum() {
        return action;
    }

    public LoanLog setAction(LoanAction action) {
        this.action = action;
        return this;
    }

    public Long getLoanId() {
        return loanId;
    }

    public LoanLog setLoanId(Long loanId) {
        this.loanId = loanId;
        return this;
    }

    public Long getAccountNumber() {
        return accountNumber;
    }

    public LoanLog setAccountNumber(Long accountNumber) {
        this.accountNumber = accountNumber;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public LoanLog setMessage(String message) {
        this.message = message;
        return this;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public LoanLog setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
        return this;
    }

    public Long getPerformedBy() {
        return performedBy;
    }

    public LoanLog setPerformedBy(Long performedBy) {
        this.performedBy = performedBy;
        return this;
    }
}
