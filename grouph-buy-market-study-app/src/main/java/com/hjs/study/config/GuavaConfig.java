package com.hjs.study.config;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * 进程内 Guava 缓存配置。
 *
 * <p>该缓存适合保存短生命周期、允许实例间短暂不一致的数据。它只存在于当前 JVM，应用重启
 * 或实例切换后数据不会保留，不能代替 Redis 等共享缓存。</p>
 */
@Configuration
public class GuavaConfig {

    /**
     * 创建字符串键值缓存。
     *
     * <p>条目写入 3 秒后自动失效，适用于削减短时间内的重复查询；Bean 显式命名为
     * {@code cache}，便于业务组件按名称注入。</p>
     *
     * @return 线程安全的 Guava 本地缓存实例
     */
    @Bean(name = "cache")
    public Cache<String, String> cache() {
        return CacheBuilder.newBuilder()
                .expireAfterWrite(3, TimeUnit.SECONDS)
                .build();
    }

}
