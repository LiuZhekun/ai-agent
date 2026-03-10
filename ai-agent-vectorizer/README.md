# ai-agent-vectorizer

## 模块目的

将结构化数据与文档内容转换为向量索引，支撑语义检索与长期记忆。

## 功能清单

- `@VectorIndexed`：声明实体向量化策略。
- `VectorSyncService`：全量/增量同步到向量存储。
- `VectorSyncScheduler`：定时增量同步。
- 文档解析器：PDF / Word / Excel 文本提取。

## 快速使用

```java
@VectorIndexed(collection = "user_profile", fields = {"name", "email", "remark"})
public class UserEntity {
}
```

```properties
ai.agent.vectorizer.enabled=true
ai.agent.vectorizer.batch-size=200
ai.agent.vectorizer.cron-expression=0 */5 * * * ?
```

## 建议实践

- 为增量同步字段统一使用 `update_time`。
- 大文件解析建议异步执行并做分片入库。
- 生产环境请监控同步耗时和失败率。
