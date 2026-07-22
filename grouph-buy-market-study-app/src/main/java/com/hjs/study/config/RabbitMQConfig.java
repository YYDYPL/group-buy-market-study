package com.hjs.study.config;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 交换机与队列的显式声明示例。
 *
 * <p>当前 {@link Configuration} 注解被注释，因此本类默认不会参与 Spring 装配；项目实际由
 * {@code @RabbitListener} 上的绑定信息声明消费队列。本类可在需要集中管理拓扑时重新启用，
 * 但应避免与监听器声明产生不一致的参数。</p>
 */
//@Configuration
public class RabbitMQConfig {

    /** 配置文件中统一的 Topic 交换机名称。 */
    @Value("${spring.rabbitmq.config.producer.exchange}")
    private String exchangeName;

    /**
     * 声明持久化、非自动删除的 Topic 交换机。
     *
     * @return 拼团业务共用的主题交换机
     */
    @Bean
    public TopicExchange topicExchange() {
        return new TopicExchange(exchangeName, true, false);
    }

    /**
     * 将“拼团成功”持久化队列绑定到主题交换机。
     *
     * @param routingKey 只接收匹配该路由键的消息
     * @param queue      配置文件指定的消费队列名称
     * @return 队列与交换机之间的绑定关系
     */
    @Bean
    public Binding topicTeamSuccessBinding(
            @Value("${spring.rabbitmq.config.producer.topic_team_success.routing_key}") String routingKey,
            @Value("${spring.rabbitmq.config.producer.topic_team_success.queue}") String queue) {
        return BindingBuilder.bind(new Queue(queue, true))
                .to(topicExchange())
                .with(routingKey);
    }

}
