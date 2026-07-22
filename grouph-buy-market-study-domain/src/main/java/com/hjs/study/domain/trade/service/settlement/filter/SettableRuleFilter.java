package com.hjs.study.domain.trade.service.settlement.filter;

import com.hjs.study.domain.trade.adapter.repository.ITradeRepository;
import com.hjs.study.domain.trade.model.entity.GroupBuyTeamEntity;
import com.hjs.study.domain.trade.model.entity.MarketPayOrderEntity;
import com.hjs.study.domain.trade.model.entity.TradeSettlementRuleCommandEntity;
import com.hjs.study.domain.trade.model.entity.TradeSettlementRuleFilterBackEntity;
import com.hjs.study.domain.trade.service.settlement.factory.TradeSettlementRuleFilterFactory;
import com.hjs.study.types.design.framework.link.model2.handler.ILogicHandler;
import com.hjs.study.types.enums.ResponseCode;
import com.hjs.study.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;

/**
 * 可结算时效校验节点。
 * <p>
 * 这一步的核心问题是：用户支付成功的时间点，是否仍然落在拼团队伍有效期内。
 * 如果支付时间已经超过团队截止时间，即使支付平台回调成功，也不能再算作有效成团交易。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 可结算规则过滤；交易时间
 * @create 2025-01-29 09:38
 */
@Slf4j
@Service
public class SettableRuleFilter implements ILogicHandler<TradeSettlementRuleCommandEntity, TradeSettlementRuleFilterFactory.DynamicContext, TradeSettlementRuleFilterBackEntity> {

    /** 交易仓储，用于查询当前预购订单所属的拼团队伍。 */
    @Resource
    private ITradeRepository repository;

    @Override
    public TradeSettlementRuleFilterBackEntity apply(TradeSettlementRuleCommandEntity requestParameter, TradeSettlementRuleFilterFactory.DynamicContext dynamicContext) throws Exception {
        log.info("结算规则过滤-有效时间校验{} outTradeNo:{}", requestParameter.getUserId(), requestParameter.getOutTradeNo());

        // 上一个节点已经把预购订单放入上下文，这里直接取出使用。
        MarketPayOrderEntity marketPayOrderEntity = dynamicContext.getMarketPayOrderEntity();

        // 结算前必须查出当前订单所属团队，才能判断有效期和团队状态。
        GroupBuyTeamEntity groupBuyTeamEntity = repository.queryGroupBuyTeamByTeamId(marketPayOrderEntity.getTeamId());

        // 外部支付完成时间必须早于团队结束时间，否则说明支付来得太晚，团队已失效。
        Date outTradeTime = requestParameter.getOutTradeTime();

        // 只接受“严格早于截止时间”的支付结果。
        if (!outTradeTime.before(groupBuyTeamEntity.getValidEndTime())) {
            log.error("订单交易时间不在拼团有效时间范围内");
            throw new AppException(ResponseCode.E0106);
        }

        // 队伍快照写回上下文，交给结束节点统一输出。
        dynamicContext.setGroupBuyTeamEntity(groupBuyTeamEntity);

        return next(requestParameter, dynamicContext);
    }

}
