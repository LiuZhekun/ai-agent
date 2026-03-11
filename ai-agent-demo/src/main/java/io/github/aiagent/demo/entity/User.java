package io.github.aiagent.demo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.github.aiagent.vectorizer.annotation.VectorIndexed;

import java.time.LocalDateTime;

/**
 * 用户实体 —— 同时演示 MyBatis-Plus 表映射与向量索引的配合使用。
 *
 * <h2>{@code @VectorIndexed} 向量索引注解说明</h2>
 * <p>
 * 标注此注解后，框架会自动将 sys_user 表中的数据向量化并同步到 Milvus，
 * 使 Agent 具备<b>语义搜索</b>能力（例如用户问"手机号 138 开头的人"也能命中）。
 * </p>
 * <ul>
 *   <li>{@code collection} — Milvus 中的 Collection 名称，建议按业务域命名</li>
 *   <li>{@code fields} — 参与向量化的字段列表（Java 属性名）</li>
 *   <li>{@code textTemplate} — 文本拼装模板，{@code {fieldName}} 占位符会被替换为字段值。
 *       自定义模板能提升语义检索质量，推荐使用</li>
 * </ul>
 *
 * <p><b>注意</b>：仅标注 {@code @VectorIndexed} 还不够，还需通过
 * {@link io.github.aiagent.demo.config.DemoVectorSyncEntityProvider} 注册实体类才会生效。</p>
 *
 * @see io.github.aiagent.vectorizer.annotation.VectorIndexed
 * @see io.github.aiagent.demo.config.DemoVectorSyncEntityProvider
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
