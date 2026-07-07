package dev.scx.sql.mysql;

import com.mysql.cj.MysqlType;
import dev.scx.sql.schema.DataTypeKind;

import java.util.Map;
import java.util.TreeMap;

import static java.lang.String.CASE_INSENSITIVE_ORDER;

/// MySQLDialectHelper
///
/// @author scx567888
final class MySQLDialectHelper {

    private final static Map<String, DataTypeKind> MAP = initMAP();

    private static Map<String, DataTypeKind> initMAP() {
        var map = new TreeMap<String, DataTypeKind>(CASE_INSENSITIVE_ORDER);

        map.put("TINYINT", DataTypeKind.TINYINT);
        map.put("TINYINT UNSIGNED", DataTypeKind.TINYINT);
        map.put("SMALLINT", DataTypeKind.SMALLINT);
        map.put("SMALLINT UNSIGNED", DataTypeKind.SMALLINT);
        map.put("MEDIUMINT UNSIGNED", DataTypeKind.INT);
        map.put("INT", DataTypeKind.INT);
        map.put("INT UNSIGNED", DataTypeKind.INT);
        map.put("BIGINT", DataTypeKind.BIGINT);
        map.put("BIGINT UNSIGNED", DataTypeKind.BIGINT);

        map.put("FLOAT", DataTypeKind.FLOAT);
        map.put("DOUBLE", DataTypeKind.DOUBLE);
        map.put("DECIMAL", DataTypeKind.DECIMAL);
        map.put("DECIMAL UNSIGNED", DataTypeKind.DECIMAL);

        map.put("BIT", DataTypeKind.BOOLEAN);

        map.put("TIME", DataTypeKind.TIME);
        map.put("DATE", DataTypeKind.DATE);
        map.put("DATETIME", DataTypeKind.DATETIME);
        map.put("TIMESTAMP", DataTypeKind.DATETIME);

        map.put("VARCHAR", DataTypeKind.VARCHAR);
        map.put("CHAR", DataTypeKind.VARCHAR);

        map.put("TEXT", DataTypeKind.TEXT);
        map.put("MEDIUMTEXT", DataTypeKind.LONGTEXT);
        map.put("LONGTEXT", DataTypeKind.LONGTEXT);

        map.put("BLOB", DataTypeKind.BLOB);
        map.put("MEDIUMBLOB", DataTypeKind.LONGBLOB);
        map.put("LONGBLOB", DataTypeKind.LONGBLOB);

        map.put("BINARY", DataTypeKind.BLOB);
        map.put("VARBINARY", DataTypeKind.BLOB);

        map.put("JSON", DataTypeKind.JSON);

        map.put("ENUM", DataTypeKind.VARCHAR);
        map.put("SET", DataTypeKind.VARCHAR);

        return map;
    }

    public static DataTypeKind dialectTypeNameToDataTypeKind(String dialectTypeName) {
        var standardDataType = MAP.get(dialectTypeName);
        if (standardDataType == null) {
            throw new IllegalArgumentException("未知方言数据类型 : " + dialectTypeName);
        }
        return standardDataType;
    }

    public static MysqlType dataTypeKindToDialectTypeName(DataTypeKind dataTypeKind) {
        return switch (dataTypeKind) {
            case TINYINT -> MysqlType.TINYINT;
            case SMALLINT -> MysqlType.SMALLINT;
            case INT -> MysqlType.INT;
            case BIGINT -> MysqlType.BIGINT;
            case FLOAT -> MysqlType.FLOAT;
            case DOUBLE -> MysqlType.DOUBLE;
            case BOOLEAN -> MysqlType.BOOLEAN;
            case DECIMAL -> MysqlType.DECIMAL;
            case DATE -> MysqlType.DATE;
            case TIME -> MysqlType.TIME;
            case DATETIME -> MysqlType.DATETIME;
            case VARCHAR -> MysqlType.VARCHAR;
            case TEXT -> MysqlType.TEXT;
            case LONGTEXT -> MysqlType.LONGTEXT;
            case BLOB -> MysqlType.BLOB;
            case LONGBLOB -> MysqlType.LONGBLOB;
            case JSON -> MysqlType.JSON;
        };
    }

}
