package com.hjs.study.infrastructure.adapter.port;

import com.hjs.study.domain.trade.adapter.port.ITradePort;
import com.hjs.study.domain.trade.model.entity.NotifyTaskEntity;
import com.hjs.study.domain.trade.model.valobj.NotifyTypeEnumVO;
import com.hjs.study.infrastructure.event.EventPublisher;
import com.hjs.study.infrastructure.gateway.GroupBuyNotifyService;
import com.hjs.study.infrastructure.redis.IRedisService;
import com.hjs.study.types.enums.NotifyTaskHTTPEnumVO;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * 交易领域端口的基础设施实现。
 * <p>
 * domain 层只定义“如何发起拼团结果通知”的抽象能力，
 * 真正通过 HTTP 回调还是 MQ 投递，由 infrastructure 层在这里决定。
 * 因此这个类本质上是一个“外部通知适配器”。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 交易接口服务
 * @create 2025-01-31 13:34
 */
@Service
public class TradePort implements ITradePort {

    /** HTTP 回调网关，负责向外部业务系统发起拼团结果通知。 */
    @Resource
    private GroupBuyNotifyService groupBuyNotifyService;
    /** Redis 能力封装，主要用于分布式锁控制。 */
    @Resource
    private IRedisService redisService;
    /** 事件发布器，用于 MQ 类型的异步通知发送。 */
    @Resource
    private EventPublisher publisher;

    /**
     * 执行拼团结果通知。
     * <p>
     * 为避免多节点部署时同一条通知任务被并发重复执行，这里先基于 Redis 锁做抢占。
     * 抢占成功后再根据通知类型选择：
     * HTTP 方式调用外部接口；
     * MQ 方式投递消息到消息中间件。
     *
     * @param notifyTask 待执行的通知任务
     * @return 通知执行结果编码：
     * {@code SUCCESS} 表示已成功投递；
     * {@code NULL} 表示本次没有抢到执行权；
     * {@code ERROR} 表示执行过程中出现异常
     * @throws Exception 由上层调用者统一感知的异常
     */
    @Override
    public String groupBuyNotify(NotifyTaskEntity notifyTask) throws Exception {
        // 锁 key 一般会把 teamId、任务类型等信息拼进去，使同一条通知任务在集群内串行执行。
        RLock lock = redisService.getLock(notifyTask.lockKey());
        try {
            // group-buy-market 拼团服务端会被部署到多台应用服务器上，那么就会有很多任务一起执行。这个时候要进行抢占，避免被多次执行
            if (lock.tryLock(3, 0, TimeUnit.SECONDS)) {
                try {
                    // 回调方式 HTTP
                    if (NotifyTypeEnumVO.HTTP.getCode().equals(notifyTask.getNotifyType())) {
                        // 无效的 notifyUrl 则直接返回成功
                        if (StringUtils.isBlank(notifyTask.getNotifyUrl()) || "暂无".equals(notifyTask.getNotifyUrl())) {
                            return NotifyTaskHTTPEnumVO.SUCCESS.getCode();
                        }
                        groupBuyNotifyService.groupBuyNotify(notifyTask.getNotifyUrl(), notifyTask.getParameterJson());
                        return NotifyTaskHTTPEnumVO.SUCCESS.getCode();
                    }

                    // 回调方式 MQ，把已经组装好的参数直接发送到指定 routingKey。
                    if (NotifyTypeEnumVO.MQ.getCode().equals(notifyTask.getNotifyType())) {
                        publisher.publish(notifyTask.getNotifyMQ(), notifyTask.getParameterJson());
                        return NotifyTaskHTTPEnumVO.SUCCESS.getCode();
                    }
                } finally {
                    if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                }
            }
            // 没抢到锁通常不算失败，只表示当前节点不是本次任务的执行者。
            return NotifyTaskHTTPEnumVO.NULL.getCode();
        } catch (Exception e) {
            // 保持现有实现语义：异常时中断当前线程，并返回错误码交由上层补偿重试。
            Thread.currentThread().interrupt();
            return NotifyTaskHTTPEnumVO.ERROR.getCode();
        }
    }

}
