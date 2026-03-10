package io.github.aiagent.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.aiagent.demo.entity.SysDict;
import io.github.aiagent.demo.mapper.SysDictMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 字典服务。
 */
@Service
public class SysDictService {
    private final SysDictMapper mapper;
    public SysDictService(SysDictMapper mapper) { this.mapper = mapper; }
    public List<SysDict> queryByType(String type) {
        return mapper.selectList(new LambdaQueryWrapper<SysDict>().eq(SysDict::getType, type));
    }
}
