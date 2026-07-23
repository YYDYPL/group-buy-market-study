package com.hjs.study.trigger.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 拼团队伍成功消息监听器。
 *
 * <p>当前实现只记录收到的消息，作为成团事件消费端的占位入口。后续可在此对接发券、履约、
 * 用户提醒等成团后的异步流程，同时保持这些流程与交易结算主链路解耦。</p>
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @create 2025-03-08 13:49
 */
@Slf4j
@Component
public class TeamSuccessTopicListener {

    /**
     * 消费拼团队伍成功事件。
     *
     * <p>队列名、交换机和路由键均从 Spring 配置读取；交换机类型为 Topic，只有匹配该路由键
     * 的消息会被投递到当前队列。</p>
     *
     * @param message 生产端发布的原始消息文本
     */
    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(value = "${spring.rabbitmq.config.producer.topic_team_success.queue}"),
                    exchange = @Exchange(value = "${spring.rabbitmq.config.producer.exchange}", type = ExchangeTypes.TOPIC),
                    key = "${spring.rabbitmq.config.producer.topic_team_success.routing_key}"
            )
    )
    public void listener(String message) {
        log.info("接收消息（组队成功）:{}", message);
    }

}
