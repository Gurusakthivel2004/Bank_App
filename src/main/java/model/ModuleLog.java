package model;

import enums.Constants.Module;

public class ModuleLog extends MarkedClass {

	private Long id;
	private Module module;
	private Long moduleId;
	private Long accountNumber;
	private String message;
	private Long createdAt;
	private Long performedBy;

	public Long getId() {
		return id;
	}

	public ModuleLog setId(Long id) {
		this.id = id;
		return this;
	}

	public String getModule() {
		return module.toString();
	}
	
	public Module getModuleEnum() {
		return module;
	}

	public ModuleLog setModule(String module) {
		if (module != null && !module.trim().isEmpty()) {
			this.module = Module.valueOf(module.toUpperCase());
		} else {
			this.module = null;
		}
		return this;
	}

	public ModuleLog setModule(Module module) {
		this.module = module;
		return this;
	}
	
	public Long getModuleId() {
		return moduleId;
	}

	public ModuleLog setModuleId(Long moduleId) {
		this.moduleId = moduleId;
		return this;
	}

	public Long getAccountNumber() {
		return accountNumber;
	}

	public ModuleLog setAccountNumber(Long accountNumber) {
		this.accountNumber = accountNumber;
		return this;
	}

	public String getMessage() {
		return message;
	}

	public ModuleLog setMessage(String message) {
		this.message = message;
		return this;
	}

	public Long getCreatedAt() {
		return createdAt;
	}

	public ModuleLog setCreatedAt(Long createdAt) {
		this.createdAt = createdAt;
		return this;
	}

	public Long getPerformedBy() {
		return performedBy;
	}

	public ModuleLog setPerformedBy(Long performedBy) {
		this.performedBy = performedBy;
		return this;
	}

}
