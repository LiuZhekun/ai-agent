package io.github.aiagent.demo.tools;

import io.github.aiagent.core.tool.annotation.AgentTool;
import io.github.aiagent.demo.entity.Department;
import io.github.aiagent.demo.service.DepartmentService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 部门工具。
 */
@Component
@AgentTool(name = "department-tools", description = "部门查询工具")
public class DepartmentTools {

    private final DepartmentService departmentService;

    public DepartmentTools(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @Tool(description = "按名称查询部门")
    public List<Department> queryDepartment(String name) {
        return departmentService.query(name);
    }

    @Tool(description = "列出全部部门")
    public List<Department> listDepartments() {
        return departmentService.listAll();
    }
}
