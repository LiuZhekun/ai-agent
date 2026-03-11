package io.github.aiagent.demo.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.aiagent.demo.entity.SysDict;
import io.github.aiagent.demo.mapper.SysDictMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 字典服务 —— 封装 sys_dict 表查询，供 {@link io.github.aiagent.demo.tools.DictTools} 调用。
 */
@Service
public class SysDictService {
    private final SysDictMapper mapper;
    public SysDictService(SysDictMapper mapper) { this.mapper = mapper; }

    /**
     * 按字典类型精确查询所有字典项。
     *
     * @param type 字典类型（如 "gender"、"status"、"grade"）
     * @return 该类型下的全部字典项
     */
    public List<SysDict> queryByType(String type) {
        return mapper.selectList(new LambdaQueryWrapper<SysDict>().eq(SysDict::getType, type));
    }
}
