package com.hjs.study;

import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 团购营销系统的 Spring Boot 启动入口。
 *
 * <p>启动类位于 {@code com.hjs.study} 根包，Spring 默认会从该包向下扫描，因此能够发现 app
 * 模块的配置类，以及 trigger、domain、infrastructure 等依赖模块中同包体系下的组件。</p>
 *
 * <p>{@link EnableScheduling} 开启定时任务支持，用于驱动通知补偿、超时退单等后台任务。</p>
 */
@SpringBootApplication
@Configurable
@EnableScheduling
public class Application {

    /**
     * 创建 Spring 应用上下文并启动内嵌 Web 容器。
     *
     * @param args 命令行参数，可用于覆盖配置文件中的 Spring Boot 属性
     */
    public static void main(String[] args){
        SpringApplication.run(Application.class);
    }

}
