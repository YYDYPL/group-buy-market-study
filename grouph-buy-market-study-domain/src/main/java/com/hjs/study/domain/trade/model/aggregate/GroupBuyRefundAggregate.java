package com.hjs.study.domain.trade.model.aggregate;

import com.hjs.study.domain.trade.model.entity.TradeRefundOrderEntity;
import com.hjs.study.domain.trade.model.valobj.GroupBuyProgressVO;
import com.hjs.study.types.enums.GroupBuyOrderEnumVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 拼团退款聚合。
 * <p>
 * 退款在这个系统里不是一个简单的“把订单状态改成已退款”动作，而是要联动判断：
 * 1. 这笔订单是否已支付。
 * 2. 当前拼团队伍是否已经成团。
 * 3. 退单后应减少锁单数还是完成数。
 * 4. 是否还要触发库存恢复、通知补偿等后续动作。
 * <p>
 * 因此这里使用聚合，把退款订单、当前拼团进度、拼团状态三类上下文放在一起，
 * 让不同退款策略可以围绕同一份业务快照执行。
 *
 * @author xiaofuge bugstack.cn @小傅哥
 * 2025/7/11 19:30
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GroupBuyRefundAggregate {

    /**
     * 退款目标订单。
     * 描述“要退哪一笔单”，包括用户、团队、活动、订单号、外部交易单号等标识信息，
     * 是所有退款处理逻辑的定位锚点。
     */
    private TradeRefundOrderEntity tradeRefundOrderEntity;

    /**
     * 当前拼团进度快照。
     * 退款并不会凭空执行，而是要结合队伍当前的锁单数、完成数来判断如何回滚。
     * 例如未支付退款通常只影响 {@code lockCount}，已支付退款则可能同时影响 {@code completeCount}。
     */
    private GroupBuyProgressVO groupBuyProgress;

    /**
     * 拼团队伍状态枚举。
     * 主要用于“已支付且已成团”的退款场景，因为这类场景下需要进一步区分：
     * 是正常成团完成，还是成团后已有失败/退款衍生状态。
     */
    private GroupBuyOrderEnumVO groupBuyOrderEnumVO;

    /**
     * 构建“未支付、未成团”退款聚合。
     * <p>
     * 这类订单通常只是锁住了名额，还没有形成真实支付完成数，
     * 因而只需要关心锁单数量的回滚。
     */
    public static GroupBuyRefundAggregate buildUnpaid2RefundAggregate(TradeRefundOrderEntity tradeRefundOrderEntity, Integer lockCount) {
        GroupBuyRefundAggregate groupBuyRefundAggregate = new GroupBuyRefundAggregate();
        groupBuyRefundAggregate.setTradeRefundOrderEntity(tradeRefundOrderEntity);
        groupBuyRefundAggregate.setGroupBuyProgress(
                GroupBuyProgressVO.builder()
                        .lockCount(lockCount)
                        .build());

        return groupBuyRefundAggregate;
    }

    /**
     * 构建“已支付、未成团”退款聚合。
     * <p>
     * 这时用户虽然支付成功，但整个团队尚未达成目标，因此退款需要同时参考：
     * 1. 当前锁单数。
     * 2. 当前已支付完成数。
     * 方便后续仓储按正确规则回滚计数。
     */
    public static GroupBuyRefundAggregate buildPaid2RefundAggregate(TradeRefundOrderEntity tradeRefundOrderEntity,
                                                                    Integer lockCount,
                                                                    Integer completeCount) {
        GroupBuyRefundAggregate groupBuyRefundAggregate = new GroupBuyRefundAggregate();
        groupBuyRefundAggregate.setTradeRefundOrderEntity(tradeRefundOrderEntity);
        groupBuyRefundAggregate.setGroupBuyProgress(
                GroupBuyProgressVO.builder()
                        .lockCount(lockCount)
                        .completeCount(completeCount)
                        .build());

        return groupBuyRefundAggregate;
    }

    /**
     * 构建“已支付、已成团”退款聚合。
     * <p>
     * 这是退款链路中最复杂的一种，因为订单所在团队已经成团，
     * 退款可能带来整团回退、通知补偿，甚至影响团队最终状态判断。
     * 所以除了进度信息外，还要额外携带队伍状态枚举供后续策略分支使用。
     */
    public static GroupBuyRefundAggregate buildPaidTeam2RefundAggregate(TradeRefundOrderEntity tradeRefundOrderEntity,
                                                                        Integer lockCount,
                                                                        Integer completeCount,
                                                                        GroupBuyOrderEnumVO groupBuyOrderEnumVO) {
        GroupBuyRefundAggregate groupBuyRefundAggregate = new GroupBuyRefundAggregate();
        groupBuyRefundAggregate.setTradeRefundOrderEntity(tradeRefundOrderEntity);
        groupBuyRefundAggregate.setGroupBuyProgress(
                GroupBuyProgressVO.builder()
                        .lockCount(lockCount)
                        .completeCount(completeCount)
                        .build());
        groupBuyRefundAggregate.setGroupBuyOrderEnumVO(groupBuyOrderEnumVO);

        return groupBuyRefundAggregate;
    }

}
