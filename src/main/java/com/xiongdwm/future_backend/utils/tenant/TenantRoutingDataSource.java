package com.xiongdwm.future_backend.utils.tenant;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class TenantRoutingDataSource extends AbstractRoutingDataSource {

    private final Map<String, DataSource> tenantDataSources = new ConcurrentHashMap<>();
    private final Map<Long, String> studioIdToDbName = new ConcurrentHashMap<>();
    private final String urlTemplate;
    private final String username;
    private final String password;
    private final String defaultDb;

    public TenantRoutingDataSource(String urlTemplate, String username, String password, String defaultDb) {
        this.urlTemplate = urlTemplate;
        this.username = username;
        this.password = password;
        this.defaultDb = defaultDb;
    }

    @Override
    protected Object determineCurrentLookupKey() {
        return TenantContext.getCurrentTenant();
    }

    @Override
    protected DataSource determineTargetDataSource() {
        String tenantDb = (String) determineCurrentLookupKey();
        if (tenantDb == null) {
            tenantDb = defaultDb;
        }
        return getOrCreateDataSource(tenantDb);
    }

    public DataSource getOrCreateDataSource(String dbName) {
        return tenantDataSources.computeIfAbsent(dbName, this::buildDataSource);
    }

    private DataSource buildDataSource(String dbName) {
        var config = new HikariConfig();
        config.setJdbcUrl(urlTemplate.replace("{db}", dbName));
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setMinimumIdle(2);
        config.setMaximumPoolSize(10);
        config.setIdleTimeout(30000);
        config.setConnectionTimeout(10000);
        config.setMaxLifetime(1800000);
        return new HikariDataSource(config);
    }

    public void registerTenant(Long studioId, String dbName) {
        studioIdToDbName.put(studioId, dbName);
        getOrCreateDataSource(dbName);
    }

    public String getDbName(Long studioId) {
        return studioIdToDbName.get(studioId);
    }

    public String getDefaultDb() {
        return defaultDb;
    }

    public String getUrlTemplate() {
        return urlTemplate;
    }

    public String getDbUsername() {
        return username;
    }

    public String getDbPassword() {
        return password;
    }

    /**
     * 创建新的 MySQL 数据库（用于新工作室注册时）
     */
    public void createDatabase(String dbName) {
        if (!dbName.matches("[a-zA-Z0-9_]+")) {
            throw new IllegalArgumentException("非法数据库名: " + dbName);
        }
        DataSource ds = getOrCreateDataSource(defaultDb);
        try (var conn = ds.getConnection();
             var stmt = conn.createStatement()) {
            stmt.execute("CREATE DATABASE IF NOT EXISTS `" + dbName
                    + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        } catch (Exception e) {
            throw new RuntimeException("创建数据库失败: " + dbName, e);
        }
    }

    /**
     * 使用 Hibernate 在目标数据库中自动建表（DDL）
     */
    public void initializeSchema(String dbName) {
        DataSource ds = getOrCreateDataSource(dbName);
        var adapter = new org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter();
        var emfBean = new org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean();
        emfBean.setDataSource(ds);
        emfBean.setPackagesToScan("com.xiongdwm.future_backend.entity");
        emfBean.setJpaVendorAdapter(adapter);
        var props = new HashMap<String, Object>();
        props.put("hibernate.hbm2ddl.auto", "update");
        props.put("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
        props.put("hibernate.physical_naming_strategy",
                "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy");
        emfBean.setJpaPropertyMap(props);
        emfBean.afterPropertiesSet();
        var emf = emfBean.getObject();
        if (emf != null) {
            emf.close();
        }
    }
}
