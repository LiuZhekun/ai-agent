package io.github.aiagent.demo.tools;

import io.github.aiagent.core.tool.annotation.AgentTool;
import io.github.aiagent.demo.entity.SysDict;
import io.github.aiagent.demo.service.SysDictService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 字典查询工具 —— 提供系统编码字典的查询能力。
 *
 * <p>
 * 当用户问"性别有哪些选项？"或"状态码都有什么含义？"时，
 * Agent 会调用 {@link #queryDict(String)} 并传入字典类型（如 "gender"、"status"）。
 * </p>
 *
 * <p>
 * 字典数据同时也被 {@code @TranslateField(type="DICT")} 引用，
 * 用于在工具调用前自动完成"男"→"M"这类名称到编码的翻译。
 * </p>
 */
@Component
@AgentTool(name = "dict-tools", description = "字典查询工具")
public class DictTools {

    private final SysDictService sysDictService;

    public DictTools(SysDictService sysDictService) {
        this.sysDictService = sysDictService;
    }

    /**
     * 按字典类型查询所有字典项。
     *
     * @param type 字典类型，如 "gender"（性别）、"status"（状态）、"grade"（等级）
     * @return 该类型下的所有字典项列表
     */
    @Tool(description = "按类型查询字典")
    public List<SysDict> queryDict(String type) {
        return sysDictService.queryByType(type);
    }
}
