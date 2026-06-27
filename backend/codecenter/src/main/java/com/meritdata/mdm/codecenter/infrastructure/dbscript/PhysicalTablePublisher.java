package com.meritdata.mdm.codecenter.infrastructure.dbscript;

import com.meritdata.mdm.codecenter.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 物理表发布器 - 跨数据库适配
 *
 * 支持: MySQL / Oracle / 达梦 DM8 / 金仓 KingbaseES / 神通 Oscar
 * 通过 dialect 探测 + SQL 模板适配
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PhysicalTablePublisher {

    private final DataSource dataSource;

    @Value("${codecenter.dbscript.dialect:auto}")
    private String configuredDialect;

    /**
     * 探测当前数据源方言
     */
    public String detectDialect() {
        if (configuredDialect != null && !"auto".equalsIgnoreCase(configuredDialect)) {
            return configuredDialect.toUpperCase();
        }
        try (Connection c = dataSource.getConnection()) {
            String name = c.getMetaData().getDatabaseProductName().toLowerCase();
            if (name.contains("mysql") || name.contains("mariadb")) return "MYSQL";
            if (name.contains("oracle")) return "ORACLE";
            if (name.contains("dm") || name.contains("dameng")) return "DAMENG";
            if (name.contains("kingbase") || name.contains("postgres")) return "KINGBASE";
            if (name.contains("oscar") || name.contains("神通")) return "SHENTONG";
            if (name.contains("h2")) return "H2";
            return name.toUpperCase();
        } catch (SQLException e) {
            log.warn("Dialect detect failed: {}", e.getMessage());
            return "MYSQL";
        }
    }

    /**
     * 检查物理表是否存在
     */
    public boolean tableExists(String tableName) {
        String dialect = detectDialect();
        try (Connection c = dataSource.getConnection()) {
            String sql = switch (dialect) {
                case "ORACLE", "DAMENG", "KINGBASE" ->
                        "SELECT COUNT(*) FROM user_tables WHERE table_name = ?";
                case "H2" -> "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = ?";
                default -> "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_NAME = ?";
            };
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, tableName.toUpperCase());
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() && rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            log.warn("Table exists check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 创建物理表（按模型属性生成 DDL）
     *
     * @param modelCode     模型编码（业务标识）
     * @param tableName     表名
     * @param attributes    属性定义
     * @return 是否创建成功
     */
    public boolean createTable(String modelCode, String tableName, List<ColumnDef> attributes) {
        if (tableExists(tableName)) {
            log.info("Table already exists, skip create: {}", tableName);
            return false;
        }
        String dialect = detectDialect();
        List<String> columnDdl = new ArrayList<>();
        for (ColumnDef attr : attributes) {
            columnDdl.add(toColumnDdl(dialect, attr));
        }
        // 追加主键列
        boolean hasPk = attributes.stream().anyMatch(a -> Boolean.TRUE.equals(a.getIsCodeField()));
        if (hasPk) {
            columnDdl.add(toPkDdl(dialect, attributes));
        }
        columnDdl.add("create_time " + timestampType(dialect) + " DEFAULT " + currentTimestamp(dialect));
        columnDdl.add("update_time " + timestampType(dialect) + " DEFAULT " + currentTimestamp(dialect));

        String sql = "CREATE TABLE " + quoteIdent(dialect, tableName) + " (\n  "
                + String.join(",\n  ", columnDdl) + "\n)";

        log.info("Create physical table: dialect={}, table={}, sql=\n{}", dialect, tableName, sql);

        try (Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.executeUpdate();
            // MySQL: 唯一索引 UNIQUE
            if ("MYSQL".equals(dialect) || "H2".equals(dialect)) {
                for (ColumnDef attr : attributes) {
                    if (Boolean.TRUE.equals(attr.getIsUnique())) {
                        String idxSql = "ALTER TABLE " + quoteIdent(dialect, tableName)
                                + " ADD CONSTRAINT " + quoteIdent(dialect, "uk_" + tableName + "_" + attr.getEnName())
                                + " UNIQUE (" + quoteIdent(dialect, attr.getEnName()) + ")";
                        try (PreparedStatement ips = c.prepareStatement(idxSql)) {
                            ips.executeUpdate();
                        }
                    }
                }
            }
            return true;
        } catch (SQLException e) {
            log.error("Create table failed: {}", sql, e);
            throw new BizException("CODECENTER-MODEL-4004",
                    "Failed to create physical table: " + e.getMessage());
        }
    }

    /**
     * 物理表列定义
     */
    public static class ColumnDef {
        private String cnName;
        private String enName;
        private String dataType;
        private Integer dataLength;
        private Integer decimalLength;
        private Boolean isRequired;
        private Boolean isUnique;
        private Boolean isCodeField;
        public String getCnName() { return cnName; }
        public void setCnName(String cnName) { this.cnName = cnName; }
        public String getEnName() { return enName; }
        public void setEnName(String enName) { this.enName = enName; }
        public String getDataType() { return dataType; }
        public void setDataType(String dataType) { this.dataType = dataType; }
        public Integer getDataLength() { return dataLength; }
        public void setDataLength(Integer dataLength) { this.dataLength = dataLength; }
        public Integer getDecimalLength() { return decimalLength; }
        public void setDecimalLength(Integer decimalLength) { this.decimalLength = decimalLength; }
        public Boolean getIsRequired() { return isRequired; }
        public void setIsRequired(Boolean isRequired) { this.isRequired = isRequired; }
        public Boolean getIsUnique() { return isUnique; }
        public void setIsUnique(Boolean isUnique) { this.isUnique = isUnique; }
        public Boolean getIsCodeField() { return isCodeField; }
        public void setIsCodeField(Boolean isCodeField) { this.isCodeField = isCodeField; }
    }

    /* =================== 私有方言适配 =================== */

    private String toColumnDdl(String dialect, ColumnDef a) {
        StringBuilder sb = new StringBuilder();
        sb.append(quoteIdent(dialect, a.getEnName())).append(" ");
        sb.append(toTypeDdl(dialect, a));
        if (Boolean.TRUE.equals(a.getIsRequired())) sb.append(" NOT NULL");
        return sb.toString();
    }

    private String toTypeDdl(String dialect, ColumnDef a) {
        String t = a.getDataType() == null ? "STRING" : a.getDataType().toUpperCase();
        int len = a.getDataLength() == null ? 0 : a.getDataLength();
        int dec = a.getDecimalLength() == null ? 0 : a.getDecimalLength();
        return switch (t) {
            case "INTEGER", "INT" -> "INT";
            case "LONG", "BIGINT" -> "BIGINT";
            case "DOUBLE", "DECIMAL", "NUMBER" -> {
                if (dec > 0) yield "DECIMAL(" + (len > 0 ? len : 18) + "," + dec + ")";
                yield "DECIMAL(" + (len > 0 ? len : 18) + ",2)";
            }
            case "DATE" -> dateType(dialect);
            case "DATETIME", "TIMESTAMP" -> timestampType(dialect);
            case "BOOLEAN", "BOOL" -> "TINYINT";
            default -> {
                if (len > 0 && len <= 4000) {
                    yield "VARCHAR(" + len + ")";
                }
                yield "TEXT";
            }
        };
    }

    private String toPkDdl(String dialect, List<ColumnDef> attributes) {
        for (ColumnDef a : attributes) {
            if (Boolean.TRUE.equals(a.getIsCodeField())) {
                return "PRIMARY KEY (" + quoteIdent(dialect, a.getEnName()) + ")";
            }
        }
        return "";
    }

    private String quoteIdent(String dialect, String ident) {
        return switch (dialect) {
            case "ORACLE", "DAMENG", "KINGBASE" -> "\"" + ident + "\"";
            default -> "`" + ident + "`";
        };
    }

    private String dateType(String dialect) {
        return switch (dialect) {
            case "ORACLE", "DAMENG", "KINGBASE" -> "DATE";
            default -> "DATE";
        };
    }

    private String timestampType(String dialect) {
        return switch (dialect) {
            case "ORACLE", "DAMENG" -> "TIMESTAMP";
            default -> "DATETIME";
        };
    }

    private String currentTimestamp(String dialect) {
        return switch (dialect) {
            case "ORACLE", "DAMENG" -> "CURRENT_TIMESTAMP";
            default -> "CURRENT_TIMESTAMP";
        };
    }
}
