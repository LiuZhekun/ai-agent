package io.github.aiagent.knowledge;

/**
 * 知识层级枚举 —— 定义 AI Agent 可注入的四层知识体系。
 * <p>
 * 层级越高，知识越接近当前任务上下文，动态性也越强：
 * <ul>
 *   <li>L0 / L1 在应用启动时加载，相对静态</li>
 *   <li>L2 / L3 在每次对话时动态生成或检索</li>
 * </ul>
 *
 * @see KnowledgeManager
 */
public enum KnowledgeLevel {
    /** L0 —— 通用背景知识：从 classpath 静态文件加载的业务术语、FAQ 等基础信息。 */
    L0,
    /** L1 —— 数据库 Schema 知识：自动发现的表结构和字段描述，帮助模型理解数据模型。 */
    L1,
    /** L2 —— 运行时工具知识：SQL 查询、API 调用等工具提供的实时业务数据。 */
    L2,
    /** L3 —— RAG 即时检索知识：根据用户当前问题动态检索并重排的文档片段。 */
    L3
}
