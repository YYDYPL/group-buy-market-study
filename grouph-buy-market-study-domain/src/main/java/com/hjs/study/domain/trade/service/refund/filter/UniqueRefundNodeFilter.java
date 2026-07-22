package com.hjs.study.domain.trade.service.refund.filter;

import com.hjs.study.domain.trade.model.entity.MarketPayOrderEntity;
import com.hjs.study.domain.trade.model.entity.TradeRefundBehaviorEntity;
import com.hjs.study.domain.trade.model.entity.TradeRefundCommandEntity;
import com.hjs.study.domain.trade.model.valobj.TradeOrderStatusEnumVO;
import com.hjs.study.domain.trade.service.refund.factory.TradeRefundRuleFilterFactory;
import com.hjs.study.types.design.framework.link.model2.handler.ILogicHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 重复退款幂等检查节点。
 * <p>
 * 退款接口非常容易被外部系统重复调用，因此在真正执行业务回滚之前，
 * 必须先判断当前订单是否已经处于关闭态。
 * 如果已经关闭，则直接返回“重复退款”结果，不再继续往后执行业务逻辑。
 *
 * @author xiaofuge bugstack.cn @小傅哥
 * 2025/7/30 10:29
 */
@Slf4j
@Service
public class UniqueRefundNodeFilter implements ILogicHandler<TradeRefundCommandEntity, TradeRefundRuleFilterFactory.DynamicContext, TradeRefundBehaviorEntity> {

    @Override
    public TradeRefundBehaviorEntity apply(TradeRefundCommandEntity tradeRefundCommandEntity, TradeRefundRuleFilterFactory.DynamicContext dynamicContext) throws Exception {
        log.info("逆向流程-退单操作，重复退单检查 userId:{} outTradeNo:{}", tradeRefundCommandEntity.getUserId(), tradeRefundCommandEntity.getOutTradeNo());

        // 订单已经由数据加载节点放入上下文。
        MarketPayOrderEntity marketPayOrderEntity = dynamicContext.getMarketPayOrderEntity();
        TradeOrderStatusEnumVO tradeOrderStatusEnumVO = marketPayOrderEntity.getTradeOrderStatusEnumVO();

        // 如果订单已经关闭，说明退款逻辑此前已经成功执行过，直接返回幂等结果。
        if (TradeOrderStatusEnumVO.CLOSE.equals(tradeOrderStatusEnumVO)) {
            log.info("逆向流程，退单操作(幂等-重复退单) userId:{} outTradeNo:{}", tradeRefundCommandEntity.getUserId(), tradeRefundCommandEntity.getOutTradeNo());
            return TradeRefundBehaviorEntity.builder()
                    .userId(tradeRefundCommandEntity.getUserId())
                    .orderId(marketPayOrderEntity.getOrderId())
                    .teamId(marketPayOrderEntity.getTeamId())
                    .tradeRefundBehaviorEnum(TradeRefundBehaviorEntity.TradeRefundBehaviorEnum.REPEAT)
                    .build();
        }
        // 仍可退款时，继续进入真正的退款策略执行节点。
        return next(tradeRefundCommandEntity, dynamicContext);
    }

}
