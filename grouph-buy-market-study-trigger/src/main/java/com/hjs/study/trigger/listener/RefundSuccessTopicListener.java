package com.hjs.study.trigger.listener;

import com.hjs.study.domain.trade.model.valobj.TeamRefundSuccess;
import com.hjs.study.domain.trade.service.ITradeRefundOrderService;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 拼团退单成功消息监听器。
 *
 * <p>退单事务完成后，生产端发布队伍退款事件；本监听器异步恢复队伍的锁单库存。把库存恢复
 * 从退单主事务拆出，可以缩短主链路耗时，并通过消息重试获得最终一致性。</p>
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @create 2025-03-08 13:49
 */
@Slf4j
@Component
public class RefundSuccessTopicListener {

    /** 退单领域服务，提供队伍锁单库存的幂等恢复能力。 */
    @Resource
    private ITradeRefundOrderService tradeRefundOrderService;

    /**
     * 消费退单成功事件并恢复对应队伍的锁单库存。
     *
     * <p>最终一致性保障：</p>
     * <ol>
     *     <li>退单事务通过本地消息表记录待发送事件，补偿任务负责确保消息最终发出；</li>
     *     <li>消费端调用库存恢复逻辑，领域服务通过分布式锁等机制防止并发重复恢复；</li>
     *     <li>处理失败时重新抛出异常，让 RabbitMQ 保持消息未确认并按配置执行重试。</li>
     * </ol>
     *
     * <p>由于消息系统通常提供“至少一次”投递，领域处理必须具备幂等性，不能依赖消息只消费
     * 一次。</p>
     *
     * @param message 序列化后的 {@link TeamRefundSuccess} 事件 JSON
     */
    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(value = "${spring.rabbitmq.config.producer.topic_team_refund.queue}"),
                    exchange = @Exchange(value = "${spring.rabbitmq.config.producer.exchange}", type = ExchangeTypes.TOPIC),
                    key = "${spring.rabbitmq.config.producer.topic_team_refund.routing_key}"
            )
    )
    public void listener(String message) {
        log.info("接收消息（退单成功）- 恢复拼团队伍锁单量:{}", message);
        // 先还原为领域值对象，避免消息协议细节扩散到领域服务内部。
        TeamRefundSuccess teamRefundSuccess = JSON.parseObject(message, TeamRefundSuccess.class);
        try {
            tradeRefundOrderService.restoreTeamLockStock(teamRefundSuccess);
        } catch (Exception e) {
            log.info("接收消息（退单成功）- 恢复拼团队伍锁单量失败:{}", message, e);
            // 不吞掉异常：只有抛出失败信号，消息容器才会按消费策略进行重试或转入死信队列。
            throw new RuntimeException(e);
        }
    }

}
