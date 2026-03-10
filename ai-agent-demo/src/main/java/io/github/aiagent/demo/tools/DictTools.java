package io.github.aiagent.demo.tools;

import io.github.aiagent.core.tool.annotation.AgentTool;
import io.github.aiagent.demo.entity.SysDict;
import io.github.aiagent.demo.service.SysDictService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 字典工具。
 */
@Component
@AgentTool(name = "dict-tools", description = "字典查询工具")
public class DictTools {

    private final SysDictService sysDictService;

    public DictTools(SysDictService sysDictService) {
        this.sysDictService = sysDictService;
    }

    @Tool(description = "按类型查询字典")
    public List<SysDict> queryDict(String type) {
        return sysDictService.queryByType(type);
    }
}
