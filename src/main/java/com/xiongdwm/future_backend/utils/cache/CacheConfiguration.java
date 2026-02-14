package com.xiongdwm.future_backend.utils.cache;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheConfiguration {

    @Bean
    public CacheHandler cacheHandler() {
        System.out.println("===========cache handler init================>>>>");
        return new CacheHandler();
    }

}
