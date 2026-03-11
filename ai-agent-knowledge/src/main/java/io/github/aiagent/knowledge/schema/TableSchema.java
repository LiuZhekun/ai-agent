package io.github.aiagent.knowledge.schema;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 表结构模型 —— 描述一张数据库表的元数据信息。
 * <p>
 * 由 {@link SchemaDiscoveryService#discover()} 从 information_schema 填充，
 * 包含表名、表注释、列定义等信息，随后由 {@link SchemaPromptGenerator} 转换为
 * 可注入 Prompt 的自然语言描述。
 * <p>
 * <b>内部类 {@link ColumnSchema}</b> 描述单个列的名称、类型和注释。
 *
 * @see SchemaDiscoveryService
 * @see SchemaPromptGenerator
 */
public class TableSchema implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private String tableName;
    private String comment;
    private List<ColumnSchema> columns = new ArrayList<>();
    // TODO 主键和外键信息尚未在 SchemaDiscoveryService.discover() 中填充，
    //  当前为空列表，后续可从 information_schema.key_column_usage 获取。
    private List<String> primaryKeys = new ArrayList<>();
    private List<String> foreignKeys = new ArrayList<>();

    /**
     * 列结构模型 —— 描述表中单个列的元数据。
     */
    public static class ColumnSchema implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        private String name;
        private String type;
        private String comment;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public List<ColumnSchema> getColumns() {
        return columns;
    }

    public void setColumns(List<ColumnSchema> columns) {
        this.columns = columns;
    }

    public List<String> getPrimaryKeys() {
        return primaryKeys;
    }

    public void setPrimaryKeys(List<String> primaryKeys) {
        this.primaryKeys = primaryKeys;
    }

    public List<String> getForeignKeys() {
        return foreignKeys;
    }

    public void setForeignKeys(List<String> foreignKeys) {
        this.foreignKeys = foreignKeys;
    }
}
