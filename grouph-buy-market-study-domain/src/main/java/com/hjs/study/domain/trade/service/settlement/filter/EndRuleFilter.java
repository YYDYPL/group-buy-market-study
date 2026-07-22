package com.hjs.study.domain.trade.service.settlement.filter;

import com.hjs.study.domain.trade.model.entity.GroupBuyTeamEntity;
import com.hjs.study.domain.trade.model.entity.TradeSettlementRuleCommandEntity;
import com.hjs.study.domain.trade.model.entity.TradeSettlementRuleFilterBackEntity;
import com.hjs.study.domain.trade.service.settlement.factory.TradeSettlementRuleFilterFactory;
import com.hjs.study.types.design.framework.link.model2.handler.ILogicHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 结算责任链结束节点。
 * <p>
 * 前面所有节点负责做校验和准备上下文，这个节点不再做额外判断，
 * 而是负责把上下文中的团队快照整理成统一的过滤结果对象返回给调用方。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 结束节点
 * @create 2025-01-29 16:37
 */
@Slf4j
@Service
public class EndRuleFilter implements ILogicHandler<TradeSettlementRuleCommandEntity, TradeSettlementRuleFilterFactory.DynamicContext, TradeSettlementRuleFilterBackEntity> {

    @Override
    public TradeSettlementRuleFilterBackEntity apply(TradeSettlementRuleCommandEntity requestParameter, TradeSettlementRuleFilterFactory.DynamicContext dynamicContext) throws Exception {
        log.info("结算规则过滤-结束节点{} outTradeNo:{}", requestParameter.getUserId(), requestParameter.getOutTradeNo());

        // 所有关键校验都通过后，直接读取上下文里的团队实体作为结算输出依据。
        GroupBuyTeamEntity groupBuyTeamEntity = dynamicContext.getGroupBuyTeamEntity();

        // 统一封装成反馈对象，供结算服务继续组装聚合与落库。
        return TradeSettlementRuleFilterBackEntity.builder()
                .teamId(groupBuyTeamEntity.getTeamId())
                .activityId(groupBuyTeamEntity.getActivityId())
                .targetCount(groupBuyTeamEntity.getTargetCount())
                .completeCount(groupBuyTeamEntity.getCompleteCount())
                .lockCount(groupBuyTeamEntity.getLockCount())
                .status(groupBuyTeamEntity.getStatus())
                .validStartTime(groupBuyTeamEntity.getValidStartTime())
                .validEndTime(groupBuyTeamEntity.getValidEndTime())
                .notifyConfigVO(groupBuyTeamEntity.getNotifyConfigVO())
                .build();
    }

}
