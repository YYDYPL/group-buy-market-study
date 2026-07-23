package com.hjs.study.trigger.job;

import com.hjs.study.domain.activity.model.entity.UserGroupBuyOrderDetailEntity;
import com.hjs.study.domain.trade.model.entity.TradeRefundCommandEntity;
import com.hjs.study.domain.trade.service.ITradeRefundOrderService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 超时未支付订单的批量退单任务。
 *
 * <p>任务周期性扫描已超过支付有效期的锁单记录，并逐笔调用统一退单领域服务释放订单与队伍
 * 资源。单笔订单失败不会中断整批处理，失败记录会保留日志并等待后续调度再次补偿。</p>
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @create 2025-01-31 15:00
 */
@Slf4j
@Service
public class TimeoutRefundJob {

    /** 退单领域服务，同时提供超时订单查询和按订单状态执行退单的能力。 */
    @Resource
    private ITradeRefundOrderService tradeRefundOrderService;

    /** Redisson 客户端，用于保证多实例环境下扫描任务不会并发重复执行。 */
    @Resource
    private RedissonClient redissonClient;

    /**
     * 每 1 分钟执行一次超时未支付订单扫描。
     *
     * <p>分布式锁固定持有 60 秒，与当前调度周期一致。任务无论正常结束、提前返回还是抛出
     * 异常，都会进入 {@code finally} 尝试释放当前线程持有的锁。</p>
     */
    @Scheduled(cron = "0 */1 * * * ?")
    public void exec() {
        // 使用所有实例共享的锁名称，确保同一时刻只有一个实例扫描和处理超时订单。
        RLock lock = redissonClient.getLock("group_buy_market_timeout_refund_job_exec");
        try {
            // 最多等待 3 秒获取锁；成功后锁的租约为 60 秒，超时将由 Redis 自动释放。
            boolean isLocked = lock.tryLock(3, 60, TimeUnit.SECONDS);
            if (!isLocked) {
                log.info("超时退单定时任务，获取锁失败，跳过本次执行");
                return;
            }

            log.info("超时退单定时任务开始执行");
            
            // 领域服务根据订单状态和支付有效期筛选需要补偿的订单。
            List<UserGroupBuyOrderDetailEntity> timeoutOrderList = tradeRefundOrderService.queryTimeoutUnpaidOrderList();
            if (timeoutOrderList == null || timeoutOrderList.isEmpty()) {
                log.info("超时退单定时任务，未发现超时未支付订单");
                return;
            }

            log.info("超时退单定时任务，发现超时未支付订单数量：{}", timeoutOrderList.size());
            
            // 分别统计成功和失败数量，便于从日志观察本轮补偿效果。
            int successCount = 0;
            int failCount = 0;
            
            // 逐单隔离异常：某一笔退单失败时继续处理剩余订单，避免整批任务被提前终止。
            for (UserGroupBuyOrderDetailEntity orderDetail : timeoutOrderList) {
                try {
                    // 复用正常退单入口，确保超时退单与接口退单遵守相同的状态机和幂等规则。
                    TradeRefundCommandEntity refundCommand = TradeRefundCommandEntity.builder()
                            .userId(orderDetail.getUserId())
                            .outTradeNo(orderDetail.getOutTradeNo())
                            .source(orderDetail.getSource())
                            .channel(orderDetail.getChannel())
                            .build();
                    
                    // 领域服务会根据订单当前状态选择策略，并释放对应的拼团锁单资源。
                    tradeRefundOrderService.refundOrder(refundCommand);
                    successCount++;
                    
                    log.info("超时订单退单成功，用户ID：{}，交易单号：{}", orderDetail.getUserId(), orderDetail.getOutTradeNo());
                    
                } catch (Exception e) {
                    failCount++;
                    log.error("超时订单退单失败，用户ID：{}，交易单号：{}，错误信息：{}", 
                            orderDetail.getUserId(), orderDetail.getOutTradeNo(), e.getMessage(), e);
                }
            }
            
            log.info("超时退单定时任务执行完成，成功：{}，失败：{}", successCount, failCount);
            
        } catch (Exception e) {
            log.error("超时退单定时任务执行异常", e);
        } finally {
            // 只有当前线程仍然持有锁时才解锁，避免非法解锁影响其他实例。
            if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

}
