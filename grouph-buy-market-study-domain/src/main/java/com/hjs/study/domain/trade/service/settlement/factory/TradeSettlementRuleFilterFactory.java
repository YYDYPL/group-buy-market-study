package com.hjs.study.domain.trade.service.settlement.factory;

import com.hjs.study.domain.trade.model.entity.GroupBuyTeamEntity;
import com.hjs.study.domain.trade.model.entity.MarketPayOrderEntity;
import com.hjs.study.domain.trade.model.entity.TradeSettlementRuleCommandEntity;
import com.hjs.study.domain.trade.model.entity.TradeSettlementRuleFilterBackEntity;
import com.hjs.study.domain.trade.service.settlement.filter.EndRuleFilter;
import com.hjs.study.domain.trade.service.settlement.filter.OutTradeNoRuleFilter;
import com.hjs.study.domain.trade.service.settlement.filter.SCRuleFilter;
import com.hjs.study.domain.trade.service.settlement.filter.SettableRuleFilter;
import com.hjs.study.types.design.framework.link.model2.LinkArmory;
import com.hjs.study.types.design.framework.link.model2.chain.BusinessLinkedList;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

/**
 * 结算规则责任链工厂。
 * <p>
 * 它负责把支付成功后的结算校验步骤按顺序装配起来，形成统一的结算责任链：
 * 1. 渠道黑名单检查。
 * 2. 外部交易单号合法性检查。
 * 3. 支付时间是否仍在拼团有效期内。
 * 4. 结束节点负责整理输出结算快照。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 交易结算规则过滤工厂
 * @create 2025-01-29 09:17
 */
@Slf4j
@Service
public class TradeSettlementRuleFilterFactory {

    @Bean("tradeSettlementRuleFilter")
    public BusinessLinkedList<TradeSettlementRuleCommandEntity,
                DynamicContext, TradeSettlementRuleFilterBackEntity> tradeSettlementRuleFilter(
            SCRuleFilter scRuleFilter,
            OutTradeNoRuleFilter outTradeNoRuleFilter,
            SettableRuleFilter settableRuleFilter,
            EndRuleFilter endRuleFilter) {

        // 责任链顺序体现业务决策顺序，越靠前的节点越偏“快速失败”。
        LinkArmory<TradeSettlementRuleCommandEntity, DynamicContext, TradeSettlementRuleFilterBackEntity> linkArmory =
                new LinkArmory<>("交易结算规则过滤链", scRuleFilter, outTradeNoRuleFilter, settableRuleFilter, endRuleFilter);

        // 产出可注入的责任链 Bean，供结算服务复用。
        return linkArmory.getLogicLink();
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DynamicContext {
        /** 外部单号反查到的预购订单实体。 */
        private MarketPayOrderEntity marketPayOrderEntity;
        /** 当前待结算的拼团队伍实体。 */
        private GroupBuyTeamEntity groupBuyTeamEntity;
    }

}
