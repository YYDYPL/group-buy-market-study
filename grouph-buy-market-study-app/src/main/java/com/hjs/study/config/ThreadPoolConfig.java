package com.hjs.study.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.*;

/**
 * 应用异步任务线程池配置。
 *
 * <p>线程数、队列容量和拒绝策略通过 {@link ThreadPoolConfigProperties} 外部化配置。
 * {@link EnableAsync} 同时开启 Spring {@code @Async} 方法的异步执行能力。</p>
 */
@Slf4j
@EnableAsync
@Configuration
@EnableConfigurationProperties(ThreadPoolConfigProperties.class)
public class ThreadPoolConfig {

    /**
     * 创建有界队列线程池，并按配置选择任务拒绝策略。
     *
     * <p>有界队列用于限制积压任务数量，避免流量突增时无限占用内存。当核心线程、最大线程和
     * 队列均达到上限后，由拒绝策略决定抛错、丢弃或让调用线程执行。</p>
     *
     * @param properties 以 {@code thread.pool.executor.config} 为前缀绑定的线程池参数
     * @return 应用共享的线程池执行器
     * @throws ClassNotFoundException  保留的兼容异常声明
     * @throws InstantiationException 保留的兼容异常声明
     * @throws IllegalAccessException 保留的兼容异常声明
     */
    @Bean
    @ConditionalOnMissingBean(ThreadPoolExecutor.class)
    public ThreadPoolExecutor threadPoolExecutor(ThreadPoolConfigProperties properties) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        // 将配置字符串映射为 JDK 提供的拒绝策略；未知值按 AbortPolicy 处理，确保失败可见。
        RejectedExecutionHandler handler;
        switch (properties.getPolicy()){
            case "AbortPolicy":
                handler = new ThreadPoolExecutor.AbortPolicy();
                break;
            case "DiscardPolicy":
                handler = new ThreadPoolExecutor.DiscardPolicy();
                break;
            case "DiscardOldestPolicy":
                handler = new ThreadPoolExecutor.DiscardOldestPolicy();
                break;
            case "CallerRunsPolicy":
                handler = new ThreadPoolExecutor.CallerRunsPolicy();
                break;
            default:
                handler = new ThreadPoolExecutor.AbortPolicy();
                break;
        }
        // keepAliveTime 按秒解释；队列使用固定容量，避免无界任务堆积。
        return new ThreadPoolExecutor(
                properties.getCorePoolSize(),
                properties.getMaxPoolSize(),
                properties.getKeepAliveTime(),
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(properties.getBlockQueueSize()),
                Executors.defaultThreadFactory(),
                handler);
    }

}
