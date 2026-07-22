package com.hjs.study.domain.trade.service.refund.business.impl;

import com.hjs.study.domain.trade.model.aggregate.GroupBuyRefundAggregate;
import com.hjs.study.domain.trade.model.entity.GroupBuyTeamEntity;
import com.hjs.study.domain.trade.model.entity.NotifyTaskEntity;
import com.hjs.study.domain.trade.model.entity.TradeRefundOrderEntity;
import com.hjs.study.domain.trade.model.valobj.TeamRefundSuccess;
import com.hjs.study.domain.trade.service.refund.business.AbstractRefundOrderStrategy;
import com.hjs.study.types.enums.GroupBuyOrderEnumVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 已支付且已成团退款策略。
 * <p>
 * 这是三种退款场景里影响面最大的一类，因为订单所属团队已经成团。
 * 退款后不仅要回滚订单与进度，还要重新判断团队最终状态：
 * 是整团失败，还是进入“成团后有退单”的衍生状态。
 *
 * @author xiaofuge bugstack.cn @小傅哥
 * 2025/7/8 07:45
 */
@Slf4j
@Service("paidTeam2RefundStrategy")
public class PaidTeam2RefundStrategy extends AbstractRefundOrderStrategy {

    @Override
    public void refundOrder(TradeRefundOrderEntity tradeRefundOrderEntity) {
        log.info("退单；已支付，已成团 userId:{} teamId:{} orderId:{}", tradeRefundOrderEntity.getUserId(), tradeRefundOrderEntity.getTeamId(), tradeRefundOrderEntity.getOrderId());

        // 已成团退款前，需要重新读取当前团队进度，判断退款后的目标状态。
        GroupBuyTeamEntity groupBuyTeamEntity = repository.queryGroupBuyTeamByTeamId(tradeRefundOrderEntity.getTeamId());
        Integer completeCount = groupBuyTeamEntity.getCompleteCount();

        // 如果当前只剩最后一笔完成单，退掉后整团就失败；
        // 否则表示“成团过，但发生了部分退款”，进入 COMPLETE_FAIL。
        GroupBuyOrderEnumVO groupBuyOrderEnumVO = 1 == completeCount ? GroupBuyOrderEnumVO.FAIL : GroupBuyOrderEnumVO.COMPLETE_FAIL;

        // 回滚时同时减少锁单数、完成数，并把团队状态更新为新的目标状态。
        NotifyTaskEntity notifyTaskEntity = repository.paidTeam2Refund(GroupBuyRefundAggregate.buildPaidTeam2RefundAggregate(tradeRefundOrderEntity, -1, -1, groupBuyOrderEnumVO));

        // 已成团退款后仍然需要给下游发送通知，告知团队状态已发生逆向变化。
        sendRefundNotifyMessage(notifyTaskEntity, "已支付，已成团");

    }

    @Override
    public void reverseStock(TeamRefundSuccess teamRefundSuccess) throws Exception {
        // 队伍已经成团结束，不再存在“可继续参团占位”的语义，因此不恢复锁单库存。
        log.info("退单；已支付、已成团，队伍组队结束，不需要恢复锁单量 {} {} {}", teamRefundSuccess.getUserId(), teamRefundSuccess.getActivityId(), teamRefundSuccess.getTeamId());
    }

}
