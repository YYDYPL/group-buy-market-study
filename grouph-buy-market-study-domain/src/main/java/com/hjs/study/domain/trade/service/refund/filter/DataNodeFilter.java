package com.hjs.study.domain.trade.service.refund.filter;

import com.hjs.study.domain.trade.adapter.repository.ITradeRepository;
import com.hjs.study.domain.trade.model.entity.GroupBuyTeamEntity;
import com.hjs.study.domain.trade.model.entity.MarketPayOrderEntity;
import com.hjs.study.domain.trade.model.entity.TradeRefundBehaviorEntity;
import com.hjs.study.domain.trade.model.entity.TradeRefundCommandEntity;
import com.hjs.study.domain.trade.service.refund.factory.TradeRefundRuleFilterFactory;
import com.hjs.study.types.design.framework.link.model2.handler.ILogicHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 退款数据加载节点。
 * <p>
 * 退款责任链的第一步不是直接退，而是先把后续所有节点都要用到的数据准备齐：
 * 1. 根据外部单号查内部预购订单。
 * 2. 根据订单查所属拼团队伍。
 * 3. 把这些数据写入上下文，交给后续节点复用。
 *
 * @author xiaofuge bugstack.cn @小傅哥
 * 2025/7/30 09:58
 */
@Slf4j
@Service
public class DataNodeFilter implements ILogicHandler<TradeRefundCommandEntity, TradeRefundRuleFilterFactory.DynamicContext, TradeRefundBehaviorEntity> {

    /** 交易仓储，负责查询退款所需的订单和团队基础数据。 */
    @Resource
    private ITradeRepository repository;

    @Override
    public TradeRefundBehaviorEntity apply(TradeRefundCommandEntity tradeRefundCommandEntity, TradeRefundRuleFilterFactory.DynamicContext dynamicContext) throws Exception {
        log.info("逆向流程-退单操作，数据加载节点 userId:{} outTradeNo:{}", tradeRefundCommandEntity.getUserId(), tradeRefundCommandEntity.getOutTradeNo());

        // 先通过外部单号定位系统内的预购订单。
        MarketPayOrderEntity marketPayOrderEntity = repository.queryMarketPayOrderEntityByOutTradeNo(tradeRefundCommandEntity.getUserId(), tradeRefundCommandEntity.getOutTradeNo());
        String teamId = marketPayOrderEntity.getTeamId();

        // 再查出该订单所属团队，以便后续判断成团状态和退款类型。
        GroupBuyTeamEntity groupBuyTeamEntity = repository.queryGroupBuyTeamByTeamId(teamId);

        // 把订单和团队快照写入上下文，减少后续节点重复查库。
        dynamicContext.setMarketPayOrderEntity(marketPayOrderEntity);
        dynamicContext.setGroupBuyTeamEntity(groupBuyTeamEntity);

        return next(tradeRefundCommandEntity, dynamicContext);
    }

}
