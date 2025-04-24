package model;

import java.math.BigDecimal;

public class SubOrg extends MarkedClass {
	
	private Long id;
	private String name;
	private Long orgId;
	private BigDecimal salaryBand;
	private Long createdAt;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Long getOrgId() {
		return orgId;
	}

	public void setOrgId(Long orgId) {
		this.orgId = orgId;
	}

	public Long getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Long createdAt) {
		this.createdAt = createdAt;
	}

	public BigDecimal getSalaryBand() {
		return salaryBand;
	}

	public void setSalaryBand(BigDecimal salaryBand) {
		this.salaryBand = salaryBand;
	}
	
}
