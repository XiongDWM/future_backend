package com.xiongdwm.future_backend.utils.cache;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class CacheConfiguration {
    private static final Logger log = LoggerFactory.getLogger(CacheConfiguration.class);

    @Bean
    public CacheHandler cacheHandler() {
        log.info("cache handler initialized");
        return new CacheHandler();
    }

}
