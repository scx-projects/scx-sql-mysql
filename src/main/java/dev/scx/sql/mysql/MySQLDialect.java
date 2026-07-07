package dev.scx.sql.mysql;

import com.mysql.cj.jdbc.MysqlDataSource;
import com.mysql.cj.jdbc.NonRegisteringDriver;
import dev.scx.sql.JDBCConnectionInfo;
import dev.scx.sql.dialect.Dialect;
import dev.scx.sql.schema.*;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.StringJoiner;

/// MySQLDialect
///
/// @author scx567888
/// @see <a href="https://dev.mysql.com/doc/refman/8.0/en/create-table.html">https://dev.mysql.com/doc/refman/8.0/en/create-table.html</a>
public final class MySQLDialect implements Dialect {

    private static final NonRegisteringDriver DRIVER = initDRIVER();

    public MySQLDialect() {

    }

    private static NonRegisteringDriver initDRIVER() {
        try {
            return new NonRegisteringDriver();
        } catch (SQLException e) {
            // 这里理论上永远不可能发生.
            throw new IllegalStateException(e);
        }
    }

    @Override
    public boolean canHandle(String url) {
        try {
            return DRIVER.acceptsURL(url);
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public boolean canHandle(DataSource dataSource) {
        try {
            return dataSource instanceof MysqlDataSource || dataSource.isWrapperFor(MysqlDataSource.class);
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public DataSource createDataSource(JDBCConnectionInfo connectionInfo) {
        var mysqlDataSource = new MysqlDataSource();
        mysqlDataSource.setUrl(connectionInfo.url());
        mysqlDataSource.setUser(connectionInfo.username());
        mysqlDataSource.setPassword(connectionInfo.password());
        // 设置参数值
        for (var parameter : connectionInfo.parameters()) {
            var p = parameter.split("=", 2);
            if (p.length == 2) {
                var property = mysqlDataSource.getProperty(p[0]);
                property.setValue(property.getPropertyDefinition().parseObject(p[1], null));
            }
        }
        return mysqlDataSource;
    }

    @Override
    public String quoteIdentifier(String identifier) {
        return "`" + identifier + "`";
    }

    @Override
    public DataTypeKind dialectTypeNameToDataTypeKind(String dialectTypeName) {
        return MySQLDialectHelper.dialectTypeNameToDataTypeKind(dialectTypeName);
    }

    @Override
    public String dataTypeKindToDialectTypeName(DataTypeKind dataTypeKind) {
        return MySQLDialectHelper.dataTypeKindToDialectTypeName(dataTypeKind).getName();
    }

    @Override
    public List<String> getCreateTableDDLs(Table table) {
        var sb = new StringBuilder();
        // 1, 标准创建表语句
        sb.append("CREATE TABLE ");
        sb.append(getQualifiedTableName(table)).append("\n");
        sb.append("(").append("\n");

        var joiner = new StringJoiner(",\n    ", "    ", "");

        // 2, 追加列定义
        for (var column : table.columns()) {
            joiner.add(getColumnDefinition(column));
        }

        // 3, 追加表约束
        for (var tableConstraint : getTableConstraints(table)) {
            joiner.add(tableConstraint);
        }

        sb.append(joiner);

        // 4, 追加 表定义 终结符
        sb.append("\n").append(");");

        return List.of(sb.toString());
    }

    @Override
    public List<String> getAddColumnDDLs(Table table, Column column) {
        var ddl = "ALTER TABLE " + getQualifiedTableName(table) + " ADD COLUMN " + getColumnDefinition(column) + ";";
        return List.of(ddl);
    }

    @Override
    public List<String> getDropColumnDDLs(Table table, Column column) {
        var ddl = "ALTER TABLE " + getQualifiedTableName(table) + " DROP COLUMN " + quoteIdentifier(column.name()) + ";";
        return List.of(ddl);
    }

    @Override
    public List<String> getAddIndexDDLs(Table table, Index index) {
        var sb = new StringBuilder();

        if (index.unique()) {
            sb.append("CREATE UNIQUE INDEX ");
        } else {
            sb.append("CREATE INDEX ");
        }

        sb.append(quoteIdentifier(index.name()))
            .append(" ON ")
            .append(getQualifiedTableName(table))
            .append(" (")
            .append(quoteIdentifier(index.columnName()))
            .append(");");

        return List.of(sb.toString());
    }

    @Override
    public List<String> getDropIndexDDLs(Table table, Index index) {
        var ddl = "DROP INDEX " + quoteIdentifier(index.name()) + " ON " + getQualifiedTableName(table) + ";";
        return List.of(ddl);
    }

    /// 获取表约束, 以语句列表形式返回. 如 `PRIMARY KEY (id)`, `UNIQUE KEY unique_name (name)`
    private List<String> getTableConstraints(Table table) {
        var list = new ArrayList<String>();

        // 记录主键中出现过的列
        var primaryKeyColumns = new HashSet<String>();

        for (var key : table.keys()) {
            var columnName = key.columnName();
            if (key.primary()) {
                primaryKeyColumns.add(columnName);
                list.add("PRIMARY KEY (" + quoteIdentifier(columnName) + ")");
            }
        }

        for (var index : table.indexes()) {
            var indexName = index.name();
            var columnName = index.columnName();

            // 已经被主键表达过了，就跳过
            if (primaryKeyColumns.contains(columnName)) {
                continue;
            }

            if (index.unique()) {
                list.add("UNIQUE KEY " + quoteIdentifier(indexName) + " (" + quoteIdentifier(columnName) + ")");
            } else {
                list.add("KEY " + quoteIdentifier(indexName) + " (" + quoteIdentifier(columnName) + ")");
            }
        }

        return list;
    }

    /// 获取列约束, 以语句列表形式返回. 如 `NOT NULL`, `AUTO_INCREMENT`
    private List<String> getColumnConstraints(Column column) {
        var list = new ArrayList<String>();

        if (column.notNull()) {
            list.add("NOT NULL");
        }

        if (column.autoIncrement()) {
            list.add("AUTO_INCREMENT");
        }

        if (column.defaultValue() != null && !column.defaultValue().isBlank()) {
            list.add("DEFAULT " + column.defaultValue());
        }

        if (column.onUpdate() != null && !column.onUpdate().isBlank()) {
            list.add("ON UPDATE " + column.onUpdate());
        }

        return list;
    }

    /// 获取数据类型定义
    private String getDataTypeDefinition(DataType dataType) {
        var typeName = dataTypeKindToDialectTypeName(dataType.kind());
        // 除了 VARCHAR 其余全部忽略 length
        if (dataType.kind() == DataTypeKind.VARCHAR) {
            var length = dataType.length();
            return length == null ? typeName : typeName + "(" + length + ")";
        } else {
            return typeName;
        }
    }

    /// 获取列定义
    private String getColumnDefinition(Column column) {
        var parts = new ArrayList<String>();

        parts.add(quoteIdentifier(column.name()));
        parts.add(getDataTypeDefinition(column.dataType()));
        parts.addAll(getColumnConstraints(column));

        return String.join(" ", parts);
    }

    private String getQualifiedTableName(Table table) {
        var schemaName = table.schema();
        var tableName = table.name();

        if (schemaName != null && !schemaName.isEmpty()) {
            return quoteIdentifier(schemaName) + "." + quoteIdentifier(tableName);
        }

        return quoteIdentifier(tableName);
    }

}
