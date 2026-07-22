package com.hjs.study.domain.trade.service.refund.business;

import com.alibaba.fastjson.JSON;
import com.hjs.study.domain.trade.adapter.repository.ITradeRepository;
import com.hjs.study.domain.trade.model.entity.NotifyTaskEntity;
import com.hjs.study.domain.trade.model.valobj.TeamRefundSuccess;
import com.hjs.study.domain.trade.service.ITradeTaskService;
import com.hjs.study.domain.trade.service.lock.factory.TradeLockRuleFilterFactory;
import com.hjs.study.types.exception.AppException;

import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 退款策略抽象基类。
 * <p>
 * 具体退款策略之间最大的区别在于“如何改状态、是否改完成数、是否恢复库存”，
 * 但它们在通知发送、库存恢复入口等辅助动作上又有很多共性。
 * 因此这里提炼出公共基类，避免各策略重复编写样板代码。
 *
 * @author xiaofuge bugstack.cn @小傅哥
 * @create 2025-01-01 00:00
 */
@Slf4j
public abstract class AbstractRefundOrderStrategy implements IRefundOrderStrategy {

    /** 交易仓储，供具体退款策略执行逆向落库与库存恢复。 */
    @Resource
    protected ITradeRepository repository;

    /** 通知任务服务，用于异步触发退款成功后的外部回调。 */
    @Resource
    protected ITradeTaskService tradeTaskService;

    /** 线程池，用于异步执行通知任务，避免阻塞退款主流程。 */
    @Resource
    protected ThreadPoolExecutor threadPoolExecutor;

    /**
     * 异步触发退款通知任务。
     * <p>
     * 虽然注释里提到 MQ，但底层实际通知方式仍由通知任务配置决定，
     * 这里的关键点是“退款主事务结束后，异步再触发外部通知”。
     *
     * @param notifyTaskEntity 通知任务实体
     * @param refundType 退单类型描述
     */
    protected void sendRefundNotifyMessage(NotifyTaskEntity notifyTaskEntity, String refundType) {
        if (null != notifyTaskEntity) {
            threadPoolExecutor.execute(() -> {
                Map<String, Integer> notifyResultMap = null;
                try {
                    notifyResultMap = tradeTaskService.execNotifyJob(notifyTaskEntity);
                    log.info("回调通知交易退单({}) result:{}", refundType, JSON.toJSONString(notifyResultMap));
                } catch (Exception e) {
                    log.error("回调通知交易退单失败({}) result:{}", refundType, JSON.toJSONString(notifyResultMap), e);
                    throw new AppException(e.getMessage());
                }
            });
        }
    }

    /**
     * 通用库存恢复逻辑。
     * <p>
     * 对于未成团但已占位的订单，退款后通常需要把团队预占名额补回去。
     * 这里统一根据活动 ID 和团队 ID 生成恢复 Key，再调用仓储处理恢复。
     *
     * @param teamRefundSuccess 团队退单成功信息
     * @param refundType 退单类型描述
     */
    protected void doReverseStock(TeamRefundSuccess teamRefundSuccess, String refundType) throws Exception {
        log.info("退单；恢复锁单量 - {} {} {} {}", refundType, teamRefundSuccess.getUserId(), teamRefundSuccess.getActivityId(), teamRefundSuccess.getTeamId());
        // 1. 恢复库存key
        String recoveryTeamStockKey = TradeLockRuleFilterFactory.generateRecoveryTeamStockKey(teamRefundSuccess.getActivityId(), teamRefundSuccess.getTeamId());
        // 2. 退单恢复库存
        repository.refund2AddRecovery(recoveryTeamStockKey, teamRefundSuccess.getOrderId());
    }

}
