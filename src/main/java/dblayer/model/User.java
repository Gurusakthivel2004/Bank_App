package dblayer.model;

public class User extends MarkedClass{

	private Long id;
    private String fullname;
    private String email;
    private Long phone;
    private String role;
    private String username;
    private String password;
    private String status;
    private Long createdAt;
    private Long modifiedAt;
    private Long performedBy;
    
    public User() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullname() {
        return fullname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Long getPhone() {
        return phone;
    }

    public void setPhone(Long phone) {
        this.phone = phone;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
    
    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long millis) {
        this.createdAt = millis;
    }
    
    public Long getModifiedAt() {
        return modifiedAt;
    }

    public void setModifiedAt(Long millis) {
        this.modifiedAt = millis;
    }
    
	public Long getPerformedBy() {
		return performedBy;
	}

	public void setPerformedBy(Long performedBy) {
		this.performedBy = performedBy;
	}
    
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", fullname='" + fullname + '\'' +
                ", email='" + email + '\'' +
                ", phone=" + phone +
                ", role='" + role + '\'' +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                ", modifiedAt=" + modifiedAt +
                ", performedBy=" + performedBy +
                '}';
    }
}
