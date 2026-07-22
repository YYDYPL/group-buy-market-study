package com.hjs.study.domain.trade.service.refund.filter;

import com.hjs.study.domain.trade.model.entity.*;
import com.hjs.study.domain.trade.model.valobj.RefundTypeEnumVO;
import com.hjs.study.domain.trade.model.valobj.TradeOrderStatusEnumVO;
import com.hjs.study.domain.trade.service.refund.business.IRefundOrderStrategy;
import com.hjs.study.domain.trade.service.refund.factory.TradeRefundRuleFilterFactory;
import com.hjs.study.types.design.framework.link.model2.handler.ILogicHandler;
import com.hjs.study.types.enums.GroupBuyOrderEnumVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Map;

/**
 * 退款执行节点。
 * <p>
 * 这是退款责任链中真正触发业务动作的节点。
 * 它会综合“订单交易状态 + 拼团队伍状态”，定位出应使用哪一种退款策略，
 * 再把退款目标订单交给对应策略去执行。
 *
 * @author xiaofuge bugstack.cn @小傅哥
 * 2025/7/30 10:31
 */
@Slf4j
@Service
public class RefundOrderNodeFilter implements ILogicHandler<TradeRefundCommandEntity, TradeRefundRuleFilterFactory.DynamicContext, TradeRefundBehaviorEntity> {

    /** 策略映射表，负责把退款场景路由到对应 Spring 策略 Bean。 */
    @Resource
    private Map<String, IRefundOrderStrategy> refundOrderStrategyMap;

    @Override
    public TradeRefundBehaviorEntity apply(TradeRefundCommandEntity tradeRefundCommandEntity, TradeRefundRuleFilterFactory.DynamicContext dynamicContext) throws Exception {
        log.info("逆向流程-退单操作，退单策略处理 userId:{} outTradeNo:{}", tradeRefundCommandEntity.getUserId(), tradeRefundCommandEntity.getOutTradeNo());

        // 先从上下文拿到订单和团队状态，这两个状态共同决定退款分支。
        MarketPayOrderEntity marketPayOrderEntity = dynamicContext.getMarketPayOrderEntity();
        TradeOrderStatusEnumVO tradeOrderStatusEnumVO = marketPayOrderEntity.getTradeOrderStatusEnumVO();

        GroupBuyTeamEntity groupBuyTeamEntity = dynamicContext.getGroupBuyTeamEntity();
        GroupBuyOrderEnumVO groupBuyOrderEnumVO = groupBuyTeamEntity.getStatus();

        // 根据“拼团队伍状态 + 交易订单状态”识别当前退款类型，并定位策略实现。
        RefundTypeEnumVO refundType = RefundTypeEnumVO.getRefundStrategy(groupBuyOrderEnumVO, tradeOrderStatusEnumVO);
        IRefundOrderStrategy refundOrderStrategy = refundOrderStrategyMap.get(refundType.getStrategy());

        // 执行退款策略时，会进一步完成状态回滚、通知任务生成、库存恢复等动作。
        refundOrderStrategy.refundOrder(TradeRefundOrderEntity.builder()
                .userId(tradeRefundCommandEntity.getUserId())
                .orderId(marketPayOrderEntity.getOrderId())
                .teamId(marketPayOrderEntity.getTeamId())
                .activityId(groupBuyTeamEntity.getActivityId())
                .outTradeNo(tradeRefundCommandEntity.getOutTradeNo())
                .build());

        // 策略执行成功后，返回统一的退款行为结果给上层。
        return TradeRefundBehaviorEntity.builder()
                .userId(tradeRefundCommandEntity.getUserId())
                .orderId(marketPayOrderEntity.getOrderId())
                .teamId(marketPayOrderEntity.getTeamId())
                .tradeRefundBehaviorEnum(TradeRefundBehaviorEntity.TradeRefundBehaviorEnum.SUCCESS)
                .build();
    }
}
