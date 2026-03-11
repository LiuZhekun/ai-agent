package io.github.aiagent.demo.tools;

import io.github.aiagent.core.tool.annotation.AgentTool;
import io.github.aiagent.demo.entity.Department;
import io.github.aiagent.demo.service.DepartmentService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 部门查询工具 —— 只读工具示例，仅提供查询能力，不涉及写操作。
 *
 * <p>
 * 与 {@link UserTools} 的区别在于本工具只暴露查询方法，适用于"辅助查询"场景。
 * 例如用户问"公司有哪些部门？"时，Agent 会调用 {@link #listDepartments()}。
 * </p>
 *
 * @see UserTools 完整 CRUD 工具示例
 */
@Component
@AgentTool(name = "department-tools", description = "部门查询工具")
public class DepartmentTools {

    private final DepartmentService departmentService;

    public DepartmentTools(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    /** 按名称模糊查询部门，name 为 null 时返回全部。 */
    @Tool(description = "按名称查询部门")
    public List<Department> queryDepartment(String name) {
        return departmentService.query(name);
    }

    /** 列出全部部门（无条件全量查询）。 */
    @Tool(description = "列出全部部门")
    public List<Department> listDepartments() {
        return departmentService.listAll();
    }
}
