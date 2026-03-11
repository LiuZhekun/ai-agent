package io.github.aiagent.demo.entity;

import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 系统字典实体 —— 对应 sys_dict 表，存储各类编码映射（性别、状态、等级等）。
 *
 * <p>
 * 字典表通过 {@code type} 分组，每组包含多条 {@code name → code} 映射。例如：
 * </p>
 * <pre>
 * type=gender: 男→M, 女→F
 * type=status: 启用→1, 禁用→0
 * </pre>
 * <p>
 * 在 DTO 中使用 {@code @TranslateField(type="DICT", target="gender")} 即可让框架
 * 自动完成"男"→"M"的翻译。
 * </p>
 *
 * @see io.github.aiagent.demo.dto.UserDTO#gender UserDTO 中的字典翻译示例
 */
@TableName("sys_dict")
public class SysDict {
    /** 字典主键 ID。 */
    private Long id;
    /** 字典类型。 */
    private String type;
    /** 字典名称。 */
    private String name;
    /** 字典编码。 */
    private String code;
    /** 排序值。 */
    private Integer sort;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getType() { return type; } public void setType(String type) { this.type = type; }
    public String getName() { return name; } public void setName(String name) { this.name = name; }
    public String getCode() { return code; } public void setCode(String code) { this.code = code; }
    public Integer getSort() { return sort; } public void setSort(Integer sort) { this.sort = sort; }
}
