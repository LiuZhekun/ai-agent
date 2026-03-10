package io.github.aiagent.demo.dto;

import io.github.aiagent.translator.annotation.TranslateField;

/**
 * 用户 DTO。
 */
public class UserDTO {
    private String name;
    private String phone;
    private String email;
    @TranslateField(type = "DICT", source = "genderName", target = "gender")
    private String gender;
    @TranslateField(type = "ENTITY_REF", source = "departmentName", target = "sys_department")
    private Long deptId;
    private Integer status;

    public String getName() { return name; } public void setName(String name) { this.name = name; }
    public String getPhone() { return phone; } public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; } public void setEmail(String email) { this.email = email; }
    public String getGender() { return gender; } public void setGender(String gender) { this.gender = gender; }
    public Long getDeptId() { return deptId; } public void setDeptId(Long deptId) { this.deptId = deptId; }
    public Integer getStatus() { return status; } public void setStatus(Integer status) { this.status = status; }
}
