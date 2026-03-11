package io.github.aiagent.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.aiagent.demo.entity.User;
import io.github.aiagent.demo.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户服务 —— 封装用户实体的基础 CRUD，供工具层（{@link io.github.aiagent.demo.tools.UserTools}）调用。
 *
 * <p>
 * <b>分层建议</b>：Service 层负责纯粹的业务逻辑和数据访问，不感知 AI Agent 的存在。
 * 工具层（Tools）负责将 Service 的能力通过 {@code @Tool} 注解暴露给 Agent。
 * 这种分层使得同一套 Service 既能被 AI 调用，也能被 REST Controller 等传统入口复用。
 * </p>
 */
@Service
public class UserService {
    private final UserMapper userMapper;
    public UserService(UserMapper userMapper) { this.userMapper = userMapper; }

    /**
     * 条件查询用户。
     *
     * @param name  姓名关键字（模糊匹配），为 null 或空串时忽略
     * @param phone 手机号关键字（模糊匹配），为 null 或空串时忽略
     * @return 符合条件的用户列表；两个参数都为空时返回全部用户
     */
    public List<User> query(String name, String phone) {
        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<>();
        if (name != null && !name.isBlank()) { qw.like(User::getName, name); }
        if (phone != null && !phone.isBlank()) { qw.like(User::getPhone, phone); }
        return userMapper.selectList(qw);
    }

    public void add(User user) { userMapper.insert(user); }
    public void update(User user) { userMapper.updateById(user); }
    public void delete(Long id) { userMapper.deleteById(id); }
}
