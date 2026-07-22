package com.hjs.study.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Redisson 单机客户端的外部化配置属性。
 *
 * <p>字段通过宽松绑定从 {@code redis.sdk.config} 读取，例如 YAML 中的 {@code pool-size}
 * 会绑定到 {@link #poolSize}。未配置的字段使用这里声明的默认值。</p>
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @create 2023-12-23 09:51
 */
@Data
@ConfigurationProperties(prefix = "redis.sdk.config", ignoreInvalidFields = true)
public class RedisClientConfigProperties {

    /** Redis 服务器主机名或 IP，不包含协议和端口。 */
    private String host;
    /** Redis 服务端口。 */
    private int port;
    /** Redis 访问密码；服务端未开启鉴权时可为空。 */
    private String password;
    /** 最大连接池容量，默认 64。 */
    private int poolSize = 64;
    /** 启动后维持的最小空闲连接数，默认 10。 */
    private int minIdleSize = 10;
    /** 连接最大空闲时间，单位毫秒；超过后空闲连接会被关闭，默认 10000。 */
    private int idleTimeout = 10000;
    /** 建立 Redis 连接的超时时间，单位毫秒，默认 10000。 */
    private int connectTimeout = 10000;
    /** 命令失败后的最大重试次数，默认 3。 */
    private int retryAttempts = 3;
    /** 两次重试之间的等待时间，单位毫秒，默认 1000。 */
    private int retryInterval = 1000;
    /** 定期发送 PING 检查连接的间隔，单位毫秒；0 表示关闭定期检查。 */
    private int pingInterval = 0;
    /** 是否开启底层 TCP keepalive，默认开启。 */
    private boolean keepAlive = true;

}
