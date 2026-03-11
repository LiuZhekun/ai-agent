package io.github.aiagent.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.aiagent.demo.entity.Department;
import io.github.aiagent.demo.mapper.DepartmentMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 部门服务 —— 封装部门表的查询操作，供 {@link io.github.aiagent.demo.tools.DepartmentTools} 调用。
 */
@Service
public class DepartmentService {
    private final DepartmentMapper departmentMapper;
    public DepartmentService(DepartmentMapper departmentMapper) { this.departmentMapper = departmentMapper; }

    /**
     * 按名称模糊查询部门。
     *
     * @param name 部门名称关键字，为 null 或空串时返回全部
     */
    public List<Department> query(String name) {
        LambdaQueryWrapper<Department> qw = new LambdaQueryWrapper<>();
        if (name != null && !name.isBlank()) { qw.like(Department::getName, name); }
        return departmentMapper.selectList(qw);
    }

    /** 查询全部部门（无条件）。 */
    public List<Department> listAll() { return departmentMapper.selectList(new LambdaQueryWrapper<>()); }
}
