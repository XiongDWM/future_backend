package com.xiongdwm.future_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceInitializationAutoConfiguration;

@SpringBootApplication(exclude = {
	DataSourceAutoConfiguration.class,
	HibernateJpaAutoConfiguration.class,
	DataSourceInitializationAutoConfiguration.class
})
public class FutureBackendApplication {
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(FutureBackendApplication.class);
	public static void main(String[] args) {
		SpringApplication.run(FutureBackendApplication.class, args);

		// 打印 GC 类型
		String gcName = java.lang.management.ManagementFactory.getGarbageCollectorMXBeans().getFirst().getName();
		logger.info("[GC] Using " + gcName);
	}

}
