package io.github.aiagent.demo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.aiagent.demo.entity.User;

/**
 * 用户 Mapper —— 继承 MyBatis-Plus {@link BaseMapper}，自动获得单表 CRUD 能力，无需手写 SQL。
 */
public interface UserMapper extends BaseMapper<User> {
}
