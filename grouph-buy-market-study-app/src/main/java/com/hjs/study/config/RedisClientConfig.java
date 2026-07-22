package com.hjs.study.config;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.BaseCodec;
import org.redisson.client.protocol.Decoder;
import org.redisson.client.protocol.Encoder;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.io.IOException;

/**
 * Redisson 客户端装配配置。
 *
 * <p>根据 {@link RedisClientConfigProperties} 创建单机模式客户端，为缓存、分布式锁、位图和
 * 发布订阅等基础设施能力提供统一 Redis 连接。连接参数集中从外部配置读取，便于不同环境
 * 独立调整。</p>
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 */
@Configuration
@EnableConfigurationProperties(RedisClientConfigProperties.class)
public class RedisClientConfig {

    /**
     * 创建应用共享的 Redisson 客户端。
     *
     * <p>{@link Primary} 指定存在多个同类型 Bean 时优先注入本实例；
     * {@link ConditionalOnMissingBean} 允许外部配置提供自定义客户端并覆盖默认实现。</p>
     *
     * @param applicationContext 当前 Spring 上下文，保留给需要读取环境或生命周期信息的扩展逻辑
     * @param properties         以 {@code redis.sdk.config} 为前缀绑定的连接参数
     * @return 已连接到单机 Redis 节点的 Redisson 客户端
     */
    @Bean("redissonClient")
    @Primary
    @ConditionalOnMissingBean(RedissonClient.class)
    public RedissonClient redissonClient(ConfigurableApplicationContext applicationContext, RedisClientConfigProperties properties) {
        Config config = new Config();
        // 使用 Jackson JSON 编解码，使 Redis 中的复杂对象能够携带类型信息完成序列化和反序列化。
        config.setCodec(JsonJacksonCodec.INSTANCE);

        // 组装单机连接地址、连接池容量、超时、重试和 TCP 保活参数。
        config.useSingleServer()
                .setAddress("redis://" + properties.getHost() + ":" + properties.getPort())
//                如 Redis 开启鉴权，可取消注释并从配置属性读取密码。
//                .setPassword(properties.getPassword())
                .setConnectionPoolSize(properties.getPoolSize())
                .setConnectionMinimumIdleSize(properties.getMinIdleSize())
                .setIdleConnectionTimeout(properties.getIdleTimeout())
                .setConnectTimeout(properties.getConnectTimeout())
                .setRetryAttempts(properties.getRetryAttempts())
                .setRetryInterval(properties.getRetryInterval())
                .setPingConnectionInterval(properties.getPingInterval())
                .setKeepAlive(properties.isKeepAlive())
        ;

        return Redisson.create(config);
    }

    /**
     * 基于 Fastjson 的备用 Redisson 编解码器。
     *
     * <p>当前客户端实际使用 {@link JsonJacksonCodec}，本实现保留用于需要兼容 Fastjson 历史数据
     * 的场景。编码失败时会主动释放 Netty 缓冲区，避免直接内存泄漏。</p>
     */
    static class RedisCodec extends BaseCodec {

        /** 将 Java 对象连同类型信息写入 Netty 字节缓冲区。 */
        private final Encoder encoder = in -> {
            ByteBuf out = ByteBufAllocator.DEFAULT.buffer();
            try {
                ByteBufOutputStream os = new ByteBufOutputStream(out);
                JSON.writeJSONString(os, in, SerializerFeature.WriteClassName);
                return os.buffer();
            } catch (IOException e) {
                out.release();
                throw e;
            } catch (Exception e) {
                out.release();
                throw new IOException(e);
            }
        };

        /** 将 Redis 返回的字节流反序列化为 Java 对象。 */
        private final Decoder<Object> decoder = (buf, state) -> JSON.parseObject(new ByteBufInputStream(buf), Object.class);

        /** @return Redis 值反序列化器 */
        @Override
        public Decoder<Object> getValueDecoder() {
            return decoder;
        }

        /** @return Redis 值序列化器 */
        @Override
        public Encoder getValueEncoder() {
            return encoder;
        }

    }

}
