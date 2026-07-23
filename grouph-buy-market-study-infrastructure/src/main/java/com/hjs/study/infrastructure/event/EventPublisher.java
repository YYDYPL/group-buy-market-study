package com.hjs.study.infrastructure.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * MQ 事件发布器。
 * <p>
 * 该类负责把系统内部事件投递到 RabbitMQ，
 * 当前主要用于拼团成功、退款等场景下的异步通知发送。
 * 上层调用方只需要提供 routingKey 和消息体，
 * 交换机名称、消息持久化等细节由这里统一处理。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 消息发送
 * @create 2024-03-30 12:40
 */
@Slf4j
@Component
public class EventPublisher {

    /** RabbitMQ 模板对象，负责完成消息发送。 */
    @Autowired
    private RabbitTemplate rabbitTemplate;

    /** 消息投递目标交换机，由配置文件统一注入。 */
    @Value("${spring.rabbitmq.config.producer.exchange}")
    private String exchangeName;

    /**
     * 发布一条 MQ 消息。
     * <p>
     * 发送时会把消息属性设置为持久化模式，尽量降低 Broker 重启导致消息丢失的风险。
     * 如果发送失败，异常会继续向上抛出，由调用方决定是否重试或走补偿逻辑。
     *
     * @param routingKey 路由键，用于决定消息进入哪个队列
     * @param message 消息体内容，通常为 JSON 字符串
     */
    public void publish(String routingKey, String message) {
        try {
            rabbitTemplate.convertAndSend(exchangeName, routingKey, message, m -> {
                // 持久化消息配置
                m.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                return m;
            });
        } catch (Exception e) {
            log.error("发送MQ消息失败 team_success message:{}", message, e);
            throw e;
        }
    }

}
