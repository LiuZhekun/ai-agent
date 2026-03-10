package io.github.aiagent.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.aiagent.demo.entity.User;
import io.github.aiagent.demo.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户服务。
 * 封装用户实体的基础 CRUD，供工具层与其他业务模块复用。
 */
@Service
public class UserService {
    private final UserMapper userMapper;
    public UserService(UserMapper userMapper) { this.userMapper = userMapper; }

    /**
     * 条件查询：name/phone 为空时自动忽略该条件。
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
