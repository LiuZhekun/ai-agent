package io.github.aiagent.demo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.github.aiagent.vectorizer.annotation.VectorIndexed;

import java.time.LocalDateTime;

/**
 * 用户实体。
 */
@TableName("sys_user")
@VectorIndexed(
        collection = "demo_user_profile",
        fields = {"name", "phone", "email", "gender"},
        textTemplate = "姓名:{name}\n手机号:{phone}\n邮箱:{email}\n性别:{gender}")
public class User {
    /** 用户主键 ID。 */
    private Long id;
    /** 用户姓名。 */
    private String name;
    /** 手机号。 */
    private String phone;
    /** 邮箱地址。 */
    private String email;
    /** 性别编码。 */
    private String gender;
    /** 部门 ID。 */
    private Long deptId;
    /** 状态码。 */
    private Integer status;
    /** 创建时间。 */
    private LocalDateTime createTime;
    /** 更新时间。 */
    private LocalDateTime updateTime;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getName() { return name; } public void setName(String name) { this.name = name; }
    public String getPhone() { return phone; } public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; } public void setEmail(String email) { this.email = email; }
    public String getGender() { return gender; } public void setGender(String gender) { this.gender = gender; }
    public Long getDeptId() { return deptId; } public void setDeptId(Long deptId) { this.deptId = deptId; }
    public Integer getStatus() { return status; } public void setStatus(Integer status) { this.status = status; }
    public LocalDateTime getCreateTime() { return createTime; } public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; } public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
