package com.company.openai.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("tripPlans");
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(2, TimeUnit.HOURS) // Itineraries expire after 2 hours
                .maximumSize(500)                   // Keep max 500 responses in memory
                .recordStats());
        return cacheManager;
    }
}
