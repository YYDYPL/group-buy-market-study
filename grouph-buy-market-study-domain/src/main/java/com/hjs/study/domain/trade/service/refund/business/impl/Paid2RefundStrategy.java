package com.hjs.study.domain.trade.service.refund.business.impl;

import com.hjs.study.domain.trade.model.aggregate.GroupBuyRefundAggregate;
import com.hjs.study.domain.trade.model.entity.NotifyTaskEntity;
import com.hjs.study.domain.trade.model.entity.TradeRefundOrderEntity;
import com.hjs.study.domain.trade.model.valobj.TeamRefundSuccess;
import com.hjs.study.domain.trade.service.refund.business.AbstractRefundOrderStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 已支付但未成团退款策略。
 * <p>
 * 这说明用户已经付款成功，但整个队伍还没有达到成团条件。
 * 因此退款时既要回滚锁单数，也要回滚完成数，
 * 让团队进度恢复到退款前的正确状态。
 *
 * @author xiaofuge bugstack.cn @小傅哥
 * 2025/7/8 07:43
 */
@Slf4j
@Service("paid2RefundStrategy")
public class Paid2RefundStrategy extends AbstractRefundOrderStrategy {

    @Override
    public void refundOrder(TradeRefundOrderEntity tradeRefundOrderEntity) throws Exception {
        log.info("退单；已支付，未成团 userId:{} teamId:{} orderId:{}", tradeRefundOrderEntity.getUserId(), tradeRefundOrderEntity.getTeamId(), tradeRefundOrderEntity.getOrderId());

        // 已支付未成团场景下，需要同时回滚锁单数和完成数。
        NotifyTaskEntity notifyTaskEntity = repository.paid2Refund(GroupBuyRefundAggregate.buildPaid2RefundAggregate(tradeRefundOrderEntity, -1, -1));

        // 退款成功后发通知，供下游做后续补偿或恢复逻辑。
        sendRefundNotifyMessage(notifyTaskEntity, "已支付，未成团");
    }

    @Override
    public void reverseStock(TeamRefundSuccess teamRefundSuccess) throws Exception {
        // 虽然已支付，但团队未成团时仍保有“占位”语义，因此要恢复锁单库存。
        doReverseStock(teamRefundSuccess, "已支付，未成团，但有锁单记录，要恢复锁单库存");
    }

}
