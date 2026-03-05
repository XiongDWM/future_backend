package com.xiongdwm.future_backend.utils.security;

import org.slf4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.xiongdwm.future_backend.service.UserService;
import com.xiongdwm.future_backend.utils.cache.CacheHandler;
import com.xiongdwm.future_backend.utils.cache.LRUCache;

import jakarta.annotation.PostConstruct;

@Component
public class UserActivityTracker {

    private static final String CACHE_NAME = "USER_ACTIVITY";
    private static final long IDLE_TIMEOUT_MS = 30 * 60 * 1000L;
    private static final int MAX_CAPACITY = 10000;

    private final CacheHandler cacheHandler;
    private final ApplicationContext applicationContext;
    private LRUCache<Long, Long> cache;
    private final Logger logger;

    public UserActivityTracker(CacheHandler cacheHandler, ApplicationContext applicationContext, Logger logger) {
        this.cacheHandler = cacheHandler;
        this.applicationContext = applicationContext;
        this.logger = logger;
    }

    @PostConstruct
    public void init() {
        cache = cacheHandler.getCache(CACHE_NAME, MAX_CAPACITY, IDLE_TIMEOUT_MS, (userId, lastActive) -> {
            try {
                applicationContext.getBean(UserService.class).autoLogout(userId);
            } catch (Exception e) {
                logger.info(e.getLocalizedMessage());
            }
        });
    }

    /** 记录用户活跃（滑动续期） */
    public void touch(long userId) {
        cache.put(userId, System.currentTimeMillis());
    }

    public void remove(long userId) {
        cache.remove(userId);
    }
}
