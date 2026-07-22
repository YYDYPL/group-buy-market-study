package com.hjs.study.config;

import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OkHttp 客户端装配配置。
 *
 * <p>将客户端作为单例 Bean 复用，可以共享连接池和线程资源，避免每次发送拼团回调时重复
 * 创建客户端造成连接与线程泄漏。</p>
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @create 2025-01-31 09:13
 */
@Configuration
public class OKHttpClientConfig {

    /**
     * 创建采用 OkHttp 默认连接、读写及重试参数的 HTTP 客户端。
     *
     * @return 供基础设施网关发送外部 HTTP 请求的共享客户端
     */
    @Bean
    public OkHttpClient httpClient() {
        return new OkHttpClient();
    }

}
