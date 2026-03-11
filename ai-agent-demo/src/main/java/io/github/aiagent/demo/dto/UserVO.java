package io.github.aiagent.demo.dto;

/**
 * 用户查询返回 VO —— 面向前端 / AI 的安全视图对象。
 *
 * <p>
 * 与直接返回 {@link io.github.aiagent.demo.entity.User} 相比，VO 的优势：
 * </p>
 * <ul>
 *   <li>{@code phoneMasked} — 手机号已脱敏，避免敏感信息泄露</li>
 *   <li>{@code departmentName} — 直接展示部门名称，无需前端再次查询</li>
 *   <li>不暴露 {@code createTime}、{@code updateTime} 等内部字段</li>
 * </ul>
 *
 * <p><b>建议</b>：工具方法的返回值尽量使用 VO 而非实体，控制 AI 可见的数据范围。</p>
 */
public class UserVO {
    private Long id;
    private String name;
    /** 脱敏后的手机号，如 138****1111。 */
    private String phoneMasked;
    private String email;
    /** 性别名称（男/女），而非存储编码（M/F）。 */
    private String gender;
    /** 部门名称，已关联查询。 */
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
