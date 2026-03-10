package io.github.aiagent.demo.tools;

import io.github.aiagent.core.tool.annotation.AgentTool;
import io.github.aiagent.demo.dto.UserDTO;
import io.github.aiagent.demo.entity.User;
import io.github.aiagent.demo.service.UserService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 用户管理工具。
 * 作为 Agent 可调用的业务能力封装层，负责把自然语言任务映射为用户领域操作。
 */
@Component
@AgentTool(name = "user-tools", description = "用户管理工具")
public class UserTools {

    private final UserService userService;

    public UserTools(UserService userService) {
        this.userService = userService;
    }

    @Tool(description = "按姓名/手机号查询用户")
    public List<User> queryUser(String name, String phone) {
        return userService.query(name, phone);
    }

    @Tool(description = "新增用户")
    public String addUser(UserDTO dto) {
        // DTO -> Entity 的转换集中在工具层，便于统一做输入治理。
        User user = convert(dto);
        userService.add(user);
        return "ok";
    }

    @Tool(description = "修改用户")
    public String updateUser(Long id, UserDTO dto) {
        User user = convert(dto);
        user.setId(id);
        userService.update(user);
        return "ok";
    }

    @Tool(description = "删除用户")
    public String deleteUser(Long id) {
        userService.delete(id);
        return "ok";
    }

    /**
     * 仅做字段映射，不做持久化操作。
     */
    private User convert(UserDTO dto) {
        User user = new User();
        user.setName(dto.getName());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setGender(dto.getGender());
        user.setDeptId(dto.getDeptId());
        user.setStatus(dto.getStatus());
        return user;
    }
}
