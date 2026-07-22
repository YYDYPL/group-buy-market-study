package com.hjs.study.domain.trade.service.refund.business;

import com.hjs.study.domain.trade.model.entity.TradeRefundOrderEntity;
import com.hjs.study.domain.trade.model.valobj.TeamRefundSuccess;

/**
 * 退款策略接口。
 * <p>
 * 退款并不是统一一套逻辑，因为不同场景的回滚动作并不一样：
 * 例如未支付退款只影响锁单数，已支付未成团还要回退完成数，
 * 已支付已成团甚至可能影响整团状态。
 * 所以这里使用策略模式，把不同退款场景拆成独立实现。
 *
 * @author xiaofuge bugstack.cn @小傅哥
 * 2025/7/8 07:37
 */
public interface IRefundOrderStrategy {

    /**
     * 执行对应场景下的退款主逻辑。
     *
     * @param tradeRefundOrderEntity 已定位好的退款目标订单
     */
    void refundOrder(TradeRefundOrderEntity tradeRefundOrderEntity) throws Exception;

    /**
     * 退款成功后是否需要恢复团队锁单库存。
     * <p>
     * 具体是否恢复，以及如何恢复，由各场景策略自己决定。
     *
     * @param teamRefundSuccess 退款成功消息
     */
    void reverseStock(TeamRefundSuccess teamRefundSuccess) throws Exception;

}
