package com.xiongdwm.future_backend.utils.ecc;

import com.xiongdwm.future_backend.utils.cache.CacheHandler;
import com.xiongdwm.future_backend.utils.cache.LRUCache;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;

@Configuration
public class CryptoSessionConfig {

    /** 协商完成的会话密钥: LRU<sessionId, SecretKey>，15分钟过期，最多512个 */
    public static final String CACHE_KM = "km";

    @Autowired
    private CacheHandler cacheHandler;

    @PostConstruct
    public void initCryptoCaches() {
        cacheHandler.addCache(CACHE_KM,
                new LRUCache<String, SecretKey>(512, 15 * 60 * 1000L));
                System.out.println("============cache km initiate================>>>>");
    }
}
