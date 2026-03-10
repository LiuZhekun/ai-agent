package io.github.aiagent.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.aiagent.demo.entity.Department;
import io.github.aiagent.demo.mapper.DepartmentMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 部门服务。
 */
@Service
public class DepartmentService {
    private final DepartmentMapper departmentMapper;
    public DepartmentService(DepartmentMapper departmentMapper) { this.departmentMapper = departmentMapper; }
    public List<Department> query(String name) {
        LambdaQueryWrapper<Department> qw = new LambdaQueryWrapper<>();
        if (name != null && !name.isBlank()) { qw.like(Department::getName, name); }
        return departmentMapper.selectList(qw);
    }
    public List<Department> listAll() { return departmentMapper.selectList(new LambdaQueryWrapper<>()); }
}
