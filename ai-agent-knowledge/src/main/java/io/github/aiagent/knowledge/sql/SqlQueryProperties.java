package io.github.aiagent.knowledge.sql;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 安全 SQL 配置。
 */
@Component
@ConfigurationProperties(prefix = "ai.agent.knowledge.sql")
public class SqlQueryProperties {
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
