package io.github.aiagent.knowledge.sql;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 安全 SQL 查询配置属性，对应配置前缀 {@code ai.agent.knowledge.sql}。
 * <p>
 * 控制 {@link SafeSqlQueryTool} 各安全组件的行为，包括：
 * <ul>
 *   <li>{@code allowedTables} / {@code deniedTables} —— 表级访问控制</li>
 *   <li>{@code deniedFunctions} —— 禁止调用的 SQL 函数</li>
 *   <li>{@code maxRows} —— 查询结果最大行数（强制 LIMIT）</li>
 *   <li>{@code maxScanRows} / {@code explainCheck} —— EXPLAIN 扫描行数阈值</li>
 *   <li>{@code rateLimit} —— 每分钟最大查询次数</li>
 *   <li>{@code maskColumns} —— 需要脱敏的列名列表</li>
 * </ul>
 *
 * @see SafeSqlQueryTool
 */
@Component
@ConfigurationProperties(prefix = "ai.agent.knowledge.sql")
public class SqlQueryProperties {
    /** L2 安全 SQL 工具总开关，设为 false 时 SafeSqlQueryTool 拒绝执行任何查询 */
    private boolean enabled = true;
    private List<String> allowedTables = new ArrayList<>();
    private List<String> deniedTables = new ArrayList<>();
    private List<String> deniedFunctions = new ArrayList<>(List.of("sleep", "load_file"));
    private int maxRows = 500;
    private int maxScanRows = 100000;
    private boolean explainCheck = true;
    private int rateLimit = 10;
    private int timeoutSeconds = 5;
    private List<String> maskColumns = new ArrayList<>();
    private boolean tenantFilterEnabled = false;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public List<String> getAllowedTables() { return allowedTables; }
    public void setAllowedTables(List<String> allowedTables) { this.allowedTables = allowedTables; }
    public List<String> getDeniedTables() { return deniedTables; }
    public void setDeniedTables(List<String> deniedTables) { this.deniedTables = deniedTables; }
    public List<String> getDeniedFunctions() { return deniedFunctions; }
    public void setDeniedFunctions(List<String> deniedFunctions) { this.deniedFunctions = deniedFunctions; }
    public int getMaxRows() { return maxRows; }
    public void setMaxRows(int maxRows) { this.maxRows = maxRows; }
    public int getMaxScanRows() { return maxScanRows; }
    public void setMaxScanRows(int maxScanRows) { this.maxScanRows = maxScanRows; }
    public boolean isExplainCheck() { return explainCheck; }
    public void setExplainCheck(boolean explainCheck) { this.explainCheck = explainCheck; }
    public int getRateLimit() { return rateLimit; }
    public void setRateLimit(int rateLimit) { this.rateLimit = rateLimit; }
    public int getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
    public List<String> getMaskColumns() { return maskColumns; }
    public void setMaskColumns(List<String> maskColumns) { this.maskColumns = maskColumns; }
    public boolean isTenantFilterEnabled() { return tenantFilterEnabled; }
    public void setTenantFilterEnabled(boolean tenantFilterEnabled) { this.tenantFilterEnabled = tenantFilterEnabled; }
}
