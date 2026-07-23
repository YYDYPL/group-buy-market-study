package com.hjs.study.trigger.job;

import com.hjs.study.domain.trade.service.ITradeTaskService;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 拼团完结后的回调通知补偿任务。
 *
 * <p>任务扫描通知任务表，对尚未成功送达的 HTTP 或 MQ 回调进行发送/重试。生产环境通常还会
 * 对已完成任务做定期归档或清理，避免任务表长期累积历史数据。</p>
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @create 2025-01-31 10:27
 */
@Slf4j
@Service
public class GroupBuyNotifyJob {

    /** 通知任务领域服务，封装待通知任务扫描、发送、重试次数和状态更新。 */
    @Resource
    private ITradeTaskService tradeTaskService;

    /** Redisson 客户端，用于在多实例部署下竞争任务执行权。 */
    @Resource
    private RedissonClient redissonClient;

    /**
     * 每天零点执行一次拼团回调通知任务。
     *
     * <p>所有应用实例都会收到 Spring 调度信号，但只有获取分布式锁的实例真正执行。锁使用
     * watchdog 自动续期模式，任务结束后在 {@code finally} 中由持锁线程主动释放。</p>
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void exec() {
        // 多个应用实例会同时触发相同任务，通过固定名称的分布式锁选出本轮唯一执行者。
        RLock lock = redissonClient.getLock("group_buy_market_notify_job_exec");
        try {
            // waitTime=3 秒表示最多等待 3 秒；leaseTime=0 启用 Redisson watchdog 自动续期。
            boolean isLocked = lock.tryLock(3, 0, TimeUnit.SECONDS);
            // 未获得执行权时直接结束，等待下一轮调度重新竞争。
            if (!isLocked) return;

            // 返回值通常记录不同通知结果的数量，统一序列化到日志便于任务监控。
            Map<String, Integer> result = tradeTaskService.execNotifyJob();
            log.info("定时任务，回调通知完成 result:{}", JSON.toJSONString(result));
        } catch (Exception e) {
            log.error("定时任务，回调通知完成失败", e);
        } finally {
            // 只允许当前持锁线程解锁，避免锁已过期或归属变化时误释放其他实例的锁。
            if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

}
