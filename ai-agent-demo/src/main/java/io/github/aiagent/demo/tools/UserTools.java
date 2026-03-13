package io.github.aiagent.demo.tools;

import io.github.aiagent.core.tool.annotation.AgentTool;
import io.github.aiagent.demo.dto.UserDTO;
import io.github.aiagent.demo.entity.User;
import io.github.aiagent.demo.service.UserService;
import org.checkerframework.checker.units.qual.A;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 用户管理工具 —— <b>接入 AI Agent 最核心的类</b>，演示如何将业务能力暴露给 AI。
 *
 * <h2>工具定义三步走</h2>
 * <ol>
 *   <li><b>类级别</b>：标注 {@code @AgentTool}，声明工具组名称（{@code name}）和描述。
 *       {@code name} 需要与配置白名单中的名称一致才会被 Agent 加载。</li>
 *   <li><b>方法级别</b>：每个 {@code @Tool} 方法就是 Agent 可调用的一个"技能"。
 *       {@code description} 会作为 Function Calling 的描述发送给 LLM，
 *       <b>描述质量直接影响 AI 调用的准确率</b>，建议写清楚功能、参数含义和返回值。</li>
 *   <li><b>注册白名单</b>：在 {@link io.github.aiagent.demo.config.DemoAgentConfig} 中
 *       将 {@code "user-tools"} 加入 {@code toolWhitelist}（默认已包含）。</li>
 * </ol>
 *
 * <h2>对话示例</h2>
 * <pre>
 * 用户: "帮我查一下张三的信息"
 *   → Agent 自动调用 queryUser(name="张三", phone=null)
 *
 * 用户: "新增一个用户，姓名李雷，手机号13900001234，部门技术部，性别男"
 *   → Agent 调用 addUser(dto)，框架先自动翻译"技术部"→部门ID、"男"→"M"，再执行插入
 *
 * 用户: "删除ID为5的用户"
 *   → Agent 调用 deleteUser(id=5)，若开启了 write-need-confirm 则先弹确认
 * </pre>
 *
 * <h2>注意事项</h2>
 * <ul>
 *   <li>工具方法的<b>参数名</b>会作为 JSON Schema 的字段名传给 LLM，建议用有语义的命名</li>
 *   <li>写操作（新增/修改/删除）建议返回简短字符串（如 "ok"），AI 会据此生成用户可读的回复</li>
 *   <li>查询操作返回 List/VO 等结构化数据，AI 会自动格式化为表格或文字</li>
 *   <li>DTO 上的 {@code @TranslateField} 会在工具调用<b>之前</b>自动执行翻译</li>
 * </ul>
 *
 * @see io.github.aiagent.core.tool.annotation.AgentTool 工具组注解
 * @see org.springframework.ai.tool.annotation.Tool 方法级工具注解（Spring AI 原生）
 * @see io.github.aiagent.demo.dto.UserDTO 带字段翻译的入参 DTO
 */
@Component
@AgentTool(name = "user-tools", description = "用户管理工具")
public class UserTools {

    private final UserService userService;

    public UserTools(UserService userService) {
        this.userService = userService;
    }

    /**
     * 按姓名 / 手机号模糊查询用户。
     * <p>参数均为可选，传 null 时忽略该条件；全部为 null 则查询全部用户。</p>
     */
    @Tool(description = "按姓名/手机号查询用户")
    public List<User> queryUser(String name, String phone) {
        return userService.query(name, phone);
    }

    /**
     * 新增用户。
     * <p>
     * 入参 {@link UserDTO} 中的 {@code @TranslateField} 字段会在调用前自动完成翻译：
     * "技术部" → 部门 ID，"男" → "M"。工具层无需关心翻译逻辑。
     * </p>
     */
    @Tool(description = "新增用户")
    public String addUser(UserDTO dto) {
        User user = convert(dto);
        userService.add(user);
        return "ok";
    }

    /**
     * 修改用户信息。
     *
     * @param id  待修改用户的主键 ID
     * @param dto 需要更新的字段（只传需要改的字段即可）
     */
    @Tool(description = "修改用户")
    public String updateUser(Long id, UserDTO dto) {
        User user = convert(dto);
        user.setId(id);
        userService.update(user);
        return "ok";
    }

    /**
     * 删除用户。
     * <p>若配置了 {@code ai.agent.safety.write-need-confirm=true}，
     * Agent 会在执行前要求用户确认。</p>
     *
     * @param id 待删除用户的主键 ID
     */
    @Tool(description = "删除用户")
    public String deleteUser(Long id) {
        userService.delete(id);
        return "ok";
    }

    /** DTO → Entity 字段映射，集中在工具层便于统一做输入治理。 */
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
