package io.github.aiagent.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.aiagent.demo.entity.Department;

/**
 * 部门 Mapper —— 继承 MyBatis-Plus {@link BaseMapper}，自动获得单表 CRUD 能力。
 */
public interface DepartmentMapper extends BaseMapper<Department> {
}
