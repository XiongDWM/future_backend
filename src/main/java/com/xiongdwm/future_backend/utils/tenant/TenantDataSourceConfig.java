package com.xiongdwm.future_backend.utils.tenant;

import java.util.HashMap;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import jakarta.persistence.EntityManagerFactory;

@Configuration
@EnableJpaRepositories(
    basePackages = "com.xiongdwm.future_backend.repository",
    entityManagerFactoryRef = "entityManagerFactory",
    transactionManagerRef = "transactionManager"
)
public class TenantDataSourceConfig {

    @Bean
    @Primary
    public TenantRoutingDataSource tenantDataSource(
            @Value("${tenant.datasource.url-template}") String urlTemplate,
            @Value("${tenant.datasource.default-db}") String defaultDb,
            @Value("${db.username}") String username,
            @Value("${db.password}") String password) {
        var routingDs = new TenantRoutingDataSource(urlTemplate, username, password, defaultDb);
        // 初始化默认数据源，AbstractRoutingDataSource 需要它
        routingDs.setDefaultTargetDataSource(routingDs.getOrCreateDataSource(defaultDb));
        routingDs.setTargetDataSources(new HashMap<>());
        routingDs.afterPropertiesSet();
        return routingDs;
    }

    @Bean
    @Primary
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(
            @Qualifier("tenantDataSource") DataSource ds) {
        var em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(ds);
        em.setPackagesToScan("com.xiongdwm.future_backend.entity");
        em.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        em.setPersistenceUnitName("tenant");
        var props = new HashMap<String, Object>();
        props.put("hibernate.hbm2ddl.auto", "update");
        props.put("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
        props.put("hibernate.physical_naming_strategy",
                "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy");
        em.setJpaPropertyMap(props);
        return em;
    }

    @Bean
    @Primary
    public PlatformTransactionManager transactionManager(
            @Qualifier("entityManagerFactory") EntityManagerFactory emf) {
        return new JpaTransactionManager(emf);
    }
}
