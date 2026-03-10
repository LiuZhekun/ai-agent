package io.github.aiagent.demo.entity;

import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 字典实体。
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
