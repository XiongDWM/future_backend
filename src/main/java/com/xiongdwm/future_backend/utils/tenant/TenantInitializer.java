package com.xiongdwm.future_backend.utils.tenant;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.xiongdwm.future_backend.platform.entity.Studio;
import com.xiongdwm.future_backend.platform.entity.UserStudioMapping;
import com.xiongdwm.future_backend.platform.repository.StudioRepository;
import com.xiongdwm.future_backend.platform.repository.UserStudioMappingRepository;
import com.xiongdwm.future_backend.repository.UserRepository;

/**
 * 应用启动后：
 * 1. 加载所有已有工作室，注册到路由数据源
 * 2. 首次运行时，为现有 future_db 数据自动创建默认工作室和用户映射
 */
@Component
public class TenantInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TenantInitializer.class);

    private final StudioRepository studioRepository;
    private final UserStudioMappingRepository mappingRepository;
    private final UserRepository userRepository;
    private final TenantRoutingDataSource tenantRoutingDataSource;

    public TenantInitializer(StudioRepository studioRepository,
                             UserStudioMappingRepository mappingRepository,
                             UserRepository userRepository,
                             TenantRoutingDataSource tenantRoutingDataSource) {
        this.studioRepository = studioRepository;
        this.mappingRepository = mappingRepository;
        this.userRepository = userRepository;
        this.tenantRoutingDataSource = tenantRoutingDataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        // 加载所有已有工作室
        var studios = studioRepository.findAll();
        for (var studio : studios) {
            if (studio.getDbName() == null) {
                log.warn("工作室 {} (id={}) 的 dbName 为空，跳过", studio.getName(), studio.getId());
                continue;
            }
            tenantRoutingDataSource.registerTenant(studio.getId(), studio.getDbName());
            // 每次启动时同步 schema，确保所有租户数据库的表结构与实体一致
            tenantRoutingDataSource.initializeSchema(studio.getDbName());
            log.info("已注册租户并同步 schema: {} -> {}", studio.getName(), studio.getDbName());
        }

        if (studios.isEmpty()) {
            log.info("首次启动，为现有数据创建默认工作室...");

            String defaultDb = tenantRoutingDataSource.getDefaultDb();

            var studio = new Studio();
            studio.setName("未来据点");
            studio.setDbName(defaultDb);
            studio.setPlace("成都");
            studio.setAssignDate(new Date());
            studio = studioRepository.saveAndFlush(studio);
            tenantRoutingDataSource.registerTenant(studio.getId(), studio.getDbName());

            // 为现有用户创建映射
            TenantContext.setCurrentTenant(defaultDb);
            try {
                var users = userRepository.findAll();
                for (var user : users) {
                    if (!mappingRepository.existsByUsername(user.getUsername())) {
                        var mapping = new UserStudioMapping();
                        mapping.setUsername(user.getUsername());
                        mapping.setStudioId(studio.getId());
                        // 默认工作室的 ADMIN 用户设为平台管理员
                        if (user.getRole() == com.xiongdwm.future_backend.entity.User.Role.ADMIN) {
                            mapping.setPlatformAdmin(true);
                        }
                        mappingRepository.save(mapping);
                    }
                }
                log.info("已为 {} 个用户创建工作室映射", users.size());
            } finally {
                TenantContext.clear();
            }
        }
    }
}
