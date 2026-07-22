package com.hjs.study.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 应用线程池的外部化配置属性。
 *
 * <p>通过 Spring Boot 宽松绑定读取 {@code thread.pool.executor.config} 下的配置。默认值用于
 * 配置项缺失时兜底，生产环境应结合 CPU 数、任务类型和下游承载能力进行压测调整。</p>
 */
@Data
@ConfigurationProperties(prefix = "thread.pool.executor.config", ignoreInvalidFields = true)
public class ThreadPoolConfigProperties {

    /** 常驻核心线程数；队列未满时任务通常由这些线程处理。 */
    private Integer corePoolSize = 20;
    /** 队列已满后线程池允许扩容到的最大线程数。 */
    private Integer maxPoolSize = 200;
    /** 非核心空闲线程的存活时间，配置类按秒传给线程池。 */
    private Long keepAliveTime = 10L;
    /** 等待执行任务的有界阻塞队列容量。 */
    private Integer blockQueueSize = 5000;
    /**
     * 线程池饱和时采用的拒绝策略名称：
     * <ul>
     *     <li>{@code AbortPolicy}：拒绝任务并抛出异常；</li>
     *     <li>{@code DiscardPolicy}：静默丢弃新任务；</li>
     *     <li>{@code DiscardOldestPolicy}：丢弃队列中最旧任务后重试；</li>
     *     <li>{@code CallerRunsPolicy}：由提交任务的线程同步执行，形成反压。</li>
     * </ul>
     */
    private String policy = "AbortPolicy";

}
