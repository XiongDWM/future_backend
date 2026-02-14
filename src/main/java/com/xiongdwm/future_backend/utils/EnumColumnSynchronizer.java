package com.xiongdwm.future_backend.utils;

import jakarta.persistence.Column;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class EnumColumnSynchronizer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(EnumColumnSynchronizer.class);

    private final DataSource dataSource;
    private final EntityManagerFactory emf;

    public EnumColumnSynchronizer(DataSource dataSource, EntityManagerFactory emf) {
        this.dataSource = dataSource;
        this.emf = emf;
    }

    @Override
    public void run(ApplicationArguments args) {
        System.out.println("=====EnumColumnSynchronizer run=====");
        var metamodel = emf.getMetamodel();

        try (Connection conn = dataSource.getConnection()) {
            String catalog = conn.getCatalog();

            for (var entityType : metamodel.getEntities()) {
                Class<?> javaType = entityType.getJavaType();
                String tableName = resolveTableName(javaType);

                for (Field field : javaType.getDeclaredFields()) {
                    Enumerated ann = field.getAnnotation(Enumerated.class);
                    if (ann == null || ann.value() != EnumType.STRING) continue;
                    if (!field.getType().isEnum()) continue;

                    String columnName = resolveColumnName(field);
                    @SuppressWarnings("unchecked")
                    Class<? extends Enum<?>> enumClass = (Class<? extends Enum<?>>) field.getType();

                    syncEnumColumn(conn, catalog, tableName, columnName, enumClass);
                }
            }
            System.out.println("[EnumSync] MySQL ENUM 列同步检查完成");
        } catch (Exception e) {
            log.error("[EnumSync] MySQL ENUM 列同步失败", e);
        }
    }

    private void syncEnumColumn(Connection conn, String catalog, String tableName,
                                String columnName, Class<? extends Enum<?>> enumClass) throws Exception {

        String sql = "SELECT COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT "
                + "FROM INFORMATION_SCHEMA.COLUMNS "
                + "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND COLUMN_NAME = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, catalog);
            ps.setString(2, tableName);
            ps.setString(3, columnName);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    log.warn("[EnumSync] 列 {}.{} 不存在，跳过", tableName, columnName);
                    return;
                }

                String columnType = rs.getString("COLUMN_TYPE");
                boolean nullable = "YES".equals(rs.getString("IS_NULLABLE"));
                String defaultVal = rs.getString("COLUMN_DEFAULT");

                if (!columnType.toLowerCase().startsWith("enum(")) {
                    return;
                }

                Set<String> dbValues = parseEnumValues(columnType);

                Set<String> javaValues = new LinkedHashSet<>();
                for (Enum<?> constant : enumClass.getEnumConstants()) {
                    javaValues.add(constant.name());
                }

                if (dbValues.containsAll(javaValues)) {
                    return;
                }

                Set<String> merged = new LinkedHashSet<>(dbValues);
                merged.addAll(javaValues);

                String enumDef = merged.stream()
                        .map(v -> "'" + v.replace("'", "''") + "'")
                        .collect(Collectors.joining(","));
                String nullClause = nullable ? " NULL" : " NOT NULL";
                String defaultClause = defaultVal != null ? " DEFAULT '" + defaultVal + "'" : "";

                String alter = String.format("ALTER TABLE `%s` MODIFY COLUMN `%s` ENUM(%s)%s%s",
                        tableName, columnName, enumDef, nullClause, defaultClause);

                try (var stmt = conn.createStatement()) {
                    stmt.executeUpdate(alter);

                    Set<String> added = new LinkedHashSet<>(javaValues);
                    added.removeAll(dbValues);
                    log.info("[EnumSync] 已同步 {}.{} — 新增值: {}", tableName, columnName, added);
                }
            }
        }
    }

    /**
     * 解析 MySQL COLUMN_TYPE 中的 enum 值列表。
     * 示例输入: enum('ACTIVE','PREPARE','HANGING')
     */
    private Set<String> parseEnumValues(String columnType) {
        Set<String> values = new LinkedHashSet<>();
        // 去掉 "enum(" 和 ")"
        int start = columnType.indexOf('(');
        int end = columnType.lastIndexOf(')');
        if (start < 0 || end < 0 || end <= start) return values;
        String inner = columnType.substring(start + 1, end);
        for (String part : inner.split(",")) {
            String trimmed = part.trim();
            if (trimmed.length() >= 2 && trimmed.startsWith("'") && trimmed.endsWith("'")) {
                values.add(trimmed.substring(1, trimmed.length() - 1));
            }
        }
        return values;
    }

    private String resolveTableName(Class<?> entityClass) {
        Table table = entityClass.getAnnotation(Table.class);
        if (table != null && !table.name().isEmpty()) {
            return table.name();
        }
        return camelToSnake(entityClass.getSimpleName());
    }

    private String resolveColumnName(Field field) {
        Column col = field.getAnnotation(Column.class);
        if (col != null && !col.name().isEmpty()) {
            return col.name();
        }
        return camelToSnake(field.getName());
    }

    private String camelToSnake(String name) {
        return name.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase();
    }
}
