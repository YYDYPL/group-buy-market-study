package com.hjs.study.domain.trade.service.settlement.filter;

import com.hjs.study.domain.trade.adapter.repository.ITradeRepository;
import com.hjs.study.domain.trade.model.entity.MarketPayOrderEntity;
import com.hjs.study.domain.trade.model.entity.TradeSettlementRuleCommandEntity;
import com.hjs.study.domain.trade.model.entity.TradeSettlementRuleFilterBackEntity;
import com.hjs.study.domain.trade.model.valobj.TradeOrderStatusEnumVO;
import com.hjs.study.domain.trade.service.settlement.factory.TradeSettlementRuleFilterFactory;
import com.hjs.study.types.design.framework.link.model2.handler.ILogicHandler;
import com.hjs.study.types.enums.ResponseCode;
import com.hjs.study.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 外部交易单号合法性校验节点。
 * <p>
 * 结算链路必须先确认这笔外部支付单号在系统内确实存在对应预购订单，
 * 且这笔订单还没有被关闭退款。否则再往下结算就会形成脏数据或重复入账。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 外部交易单号过滤；外部交易单号是否为退单
 * @create 2025-01-29 09:37
 */
@Slf4j
@Service
public class OutTradeNoRuleFilter implements ILogicHandler<TradeSettlementRuleCommandEntity, TradeSettlementRuleFilterFactory.DynamicContext, TradeSettlementRuleFilterBackEntity> {

    /** 交易仓储，用于根据外部单号反查内部预购订单。 */
    @Resource
    private ITradeRepository repository;

    @Override
    public TradeSettlementRuleFilterBackEntity apply(TradeSettlementRuleCommandEntity requestParameter, TradeSettlementRuleFilterFactory.DynamicContext dynamicContext) throws Exception {
        log.info("结算规则过滤-外部单号校验{} outTradeNo:{}", requestParameter.getUserId(), requestParameter.getOutTradeNo());

        // 通过“用户 + 外部单号”定位内部预购订单，确保结算目标明确。
        MarketPayOrderEntity marketPayOrderEntity = repository.queryMarketPayOrderEntityByOutTradeNo(requestParameter.getUserId(), requestParameter.getOutTradeNo());

        // 不存在订单或订单已经关闭，说明这笔支付通知不应再参与结算。
        if (null == marketPayOrderEntity || TradeOrderStatusEnumVO.CLOSE.equals(marketPayOrderEntity.getTradeOrderStatusEnumVO())) {
            log.error("不存在的外部交易单号或用户已退单，不需要做支付订单结算:{} outTradeNo:{}", requestParameter.getUserId(), requestParameter.getOutTradeNo());
            throw new AppException(ResponseCode.E0104);
        }

        // 把查到的订单放入上下文，供后续时效校验与结束节点复用。
        dynamicContext.setMarketPayOrderEntity(marketPayOrderEntity);

        return next(requestParameter, dynamicContext);
    }

}
