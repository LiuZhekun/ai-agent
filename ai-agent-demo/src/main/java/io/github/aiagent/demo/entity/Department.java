package io.github.aiagent.demo.entity;

import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 部门实体 —— 对应 sys_department 表。
 *
 * <p>
 * 在本 Demo 中，部门表的 {@code name} 字段被 {@code @TranslateField(type="ENTITY_REF")}
 * 引用，用于将用户输入的"技术部"自动翻译为部门 ID。
 * </p>
 *
 * @see io.github.aiagent.demo.dto.UserDTO#deptId UserDTO 中的引用示例
 */
@TableName("sys_department")
public class Department {
    /** 部门主键 ID。 */
    private Long id;
    /** 部门名称。 */
    private String name;
    /** 上级部门 ID。 */
    private Long parentId;
    /** 部门负责人。 */
    private String leader;
    /** 状态码。 */
    private Integer status;
    /** 排序值。 */
    private Integer sort;
    public Long getId() { return id; } public void setId(Long id) { this.id = id; }
    public String getName() { return name; } public void setName(String name) { this.name = name; }
    public Long getParentId() { return parentId; } public void setParentId(Long parentId) { this.parentId = parentId; }
    public String getLeader() { return leader; } public void setLeader(String leader) { this.leader = leader; }
    public Integer getStatus() { return status; } public void setStatus(Integer status) { this.status = status; }
    public Integer getSort() { return sort; } public void setSort(Integer sort) { this.sort = sort; }
}
