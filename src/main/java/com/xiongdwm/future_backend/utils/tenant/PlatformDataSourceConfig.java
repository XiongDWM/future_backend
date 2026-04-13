package com.xiongdwm.future_backend.utils.tenant;

import java.util.HashMap;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import jakarta.persistence.EntityManagerFactory;

@Configuration
@EnableJpaRepositories(
    basePackages = "com.xiongdwm.future_backend.platform.repository",
    entityManagerFactoryRef = "platformEntityManagerFactory",
    transactionManagerRef = "platformTransactionManager"
)
public class PlatformDataSourceConfig {

    @Bean
    public DataSource platformDataSource(
            @Value("${platform.datasource.url}") String url,
            @Value("${db.username}") String username,
            @Value("${db.password}") String password,
            @Value("${tenant.datasource.url-template}") String tenantUrlTemplate) {
        // 自动创建平台数据库（如果不存在）
        try {
            String infoSchemaUrl = tenantUrlTemplate.replace("{db}", "information_schema");
            try (var conn = java.sql.DriverManager.getConnection(infoSchemaUrl, username, password);
                 var stmt = conn.createStatement()) {
                int start = url.indexOf('/', url.indexOf("//") + 2) + 1;
                int end = url.indexOf('?', start);
                String dbName = url.substring(start, end < 0 ? url.length() : end);
                stmt.execute("CREATE DATABASE IF NOT EXISTS `" + dbName
                        + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            }
        } catch (Exception e) {
            // 数据库可能已存在，忽略
        }

        var config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setMinimumIdle(2);
        config.setMaximumPoolSize(5);
        config.setIdleTimeout(30000);
        config.setConnectionTimeout(10000);
        config.setMaxLifetime(1800000);
        return new HikariDataSource(config);
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean platformEntityManagerFactory(
            @Qualifier("platformDataSource") DataSource ds) {
        var em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(ds);
        em.setPackagesToScan("com.xiongdwm.future_backend.platform.entity");
        em.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        em.setPersistenceUnitName("platform");
        var props = new HashMap<String, Object>();
        props.put("hibernate.hbm2ddl.auto", "update");
        props.put("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
        props.put("hibernate.physical_naming_strategy",
                "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy");
        em.setJpaPropertyMap(props);
        return em;
    }

    @Bean
    public PlatformTransactionManager platformTransactionManager(
            @Qualifier("platformEntityManagerFactory") EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }
}
