package com.saasai.config;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import jakarta.annotation.PostConstruct;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

@ConditionalOnBean(org.springframework.cache.CacheManager.class)
@Component
public class CacheManagerChecker {

    private final CacheManager cacheManager;

    public CacheManagerChecker(ObjectProvider<CacheManager> cacheManagerProvider) {
        CacheManager cacheManager = cacheManagerProvider.getIfAvailable();
        if (cacheManager == null) {
            // log and skip cache-related checks
        } else {
            // use cacheManager
        }
        this.cacheManager = cacheManager;

        // cacheManagerProvider.ifAvailable(cache -> {
        //     // Chỉ chạy block này nếu có CacheManager
        //     cache.getCacheNames();
        // });
    }

    @PostConstruct
    public void printCacheManager() {
        System.out.println(
                "ACTIVE CACHE MANAGER: "
                        + cacheManager.getClass().getName()
        );
    }

    
}