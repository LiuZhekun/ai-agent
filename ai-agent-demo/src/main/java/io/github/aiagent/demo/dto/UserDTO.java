package io.github.aiagent.demo.dto;

import io.github.aiagent.translator.annotation.TranslateField;

/**
 * 用户新增/修改 DTO —— 演示 {@code @TranslateField} 字段自动翻译能力。
 *
 * <h2>什么是字段翻译？</h2>
 * <p>
 * 用户通过自然语言对话时，通常会说"性别=男"、"部门=技术部"这样的<b>可读名称</b>，
 * 而数据库存储的是编码值（{@code "M"}）或外键 ID（{@code 1L}）。
 * {@code @TranslateField} 让框架在工具调用前<b>自动完成"名称 → 编码/ID"的转换</b>，
 * 工具层无需手写翻译逻辑。
 * </p>
 *
 * <h2>两种翻译类型</h2>
 * <ul>
 *   <li><b>DICT</b> — 字典翻译：从 sys_dict 表中根据字典 name 查找对应 code。
 *       例：用户说"男" → 翻译为 {@code "M"}</li>
 *   <li><b>ENTITY_REF</b> — 实体引用翻译：从目标表（如 sys_department）中根据 name 查找 id。
 *       例：用户说"技术部" → 翻译为 {@code 1L}</li>
 * </ul>
 *
 * <h2>注解参数说明</h2>
 * <ul>
 *   <li>{@code type} — 翻译策略类型：{@code "DICT"}（字典）或 {@code "ENTITY_REF"}（实体引用）</li>
 *   <li>{@code source} — AI 传入的<b>来源字段名</b>（JSON 中的 key），通常是人类可读的名称字段</li>
 *   <li>{@code target} — 翻译的目标：DICT 类型对应字典 type 值；ENTITY_REF 类型对应目标表名</li>
 * </ul>
 *
 * @see io.github.aiagent.translator.annotation.TranslateField
 */
public class UserDTO {
    private String name;
    private String phone;
    private String email;

    /**
     * 性别编码。
     * AI 会传入 {@code genderName}（如"男"），框架自动从 sys_dict 中查找
     * type='gender' & name='男' 对应的 code='M'，赋值到本字段。
     */
    @TranslateField(type = "DICT", source = "genderName", target = "gender")
    private String gender;

    /**
     * 部门 ID（外键）。
     * AI 会传入 {@code departmentName}（如"技术部"），框架自动从 sys_department 表中
     * 查找 name='技术部' 对应的 id=1，赋值到本字段。
     */
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
