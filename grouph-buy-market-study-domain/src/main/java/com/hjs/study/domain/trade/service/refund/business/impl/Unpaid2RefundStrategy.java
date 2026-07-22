package com.hjs.study.domain.trade.service.refund.business.impl;

import com.hjs.study.domain.trade.model.aggregate.GroupBuyRefundAggregate;
import com.hjs.study.domain.trade.model.entity.NotifyTaskEntity;
import com.hjs.study.domain.trade.model.entity.TradeRefundOrderEntity;
import com.hjs.study.domain.trade.model.valobj.TeamRefundSuccess;
import com.hjs.study.domain.trade.service.refund.business.AbstractRefundOrderStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 未支付且未成团退款策略。
 * <p>
 * 这类订单只完成了锁单占位，还没有形成真实支付完成数，
 * 所以退款时通常只需要减少锁单数，并触发对应退款通知。
 *
 * @author xiaofuge bugstack.cn @小傅哥
 * 2025/7/8 07:41
 */
@Slf4j
@Service("unpaid2RefundStrategy")
public class Unpaid2RefundStrategy extends AbstractRefundOrderStrategy {

    @Override
    public void refundOrder(TradeRefundOrderEntity tradeRefundOrderEntity) {
        log.info("退单；未支付，未成团 userId:{} teamId:{} orderId:{}", tradeRefundOrderEntity.getUserId(), tradeRefundOrderEntity.getTeamId(), tradeRefundOrderEntity.getOrderId());
        // 这类场景只需要回滚锁单数量，不涉及完成数减少。
        NotifyTaskEntity notifyTaskEntity = repository.unpaid2Refund(GroupBuyRefundAggregate.buildUnpaid2RefundAggregate(tradeRefundOrderEntity, -1));

        // 退款成功后发出通知任务，后续可据此恢复团队预占库存。
        sendRefundNotifyMessage(notifyTaskEntity, "未支付，未成团");
    }

    @Override
    public void reverseStock(TeamRefundSuccess teamRefundSuccess) throws Exception {
        // 虽未支付，但只要之前占过位，退款后仍要把团队锁单库存补回去。
        doReverseStock(teamRefundSuccess, "未支付，未成团，但有锁单记录，要恢复锁单库存");
    }

}
