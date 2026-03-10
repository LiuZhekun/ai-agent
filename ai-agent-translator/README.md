# ai-agent-translator

## 模块目的

把业务输入中的“可读值”自动翻译成“系统内部值”，降低工具调用门槛。

## 双路径机制

- 路径 A：`TranslateToolCallbackDecorator` 在工具调用前自动翻译入参。
- 路径 B：提供 `TranslateTools` 让模型在规划中显式调用翻译工具。

## 使用示例

```java
public class UserDTO {
    @TranslateField(type = "DICT", source = "genderName", target = "gender")
    private String genderName;
}
```

## 自定义翻译器

实现 `FieldTranslator` 并注册为 Spring Bean 即可：

```java
@Component
public class RegionTranslator implements FieldTranslator {
    @Override
    public String getType() { return "REGION"; }
}
```

## 冲突策略

- `USER_SELECT`：返回候选，让用户选择。
- `FIRST_MATCH`：取第一条。
- `FAIL`：直接报错并要求补充条件。
