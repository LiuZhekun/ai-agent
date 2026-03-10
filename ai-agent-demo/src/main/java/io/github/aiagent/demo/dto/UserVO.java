package io.github.aiagent.demo.dto;

/**
 * 用户查询返回 VO。
 */
public class UserVO {
    private Long id;
    private String name;
    private String phoneMasked;
    private String email;
    private String gender;
    private String departmentName;
    private Integer status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhoneMasked() { return phoneMasked; }
    public void setPhoneMasked(String phoneMasked) { this.phoneMasked = phoneMasked; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
