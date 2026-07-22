package com.hjs.study.domain.trade.service.refund.factory;

import com.hjs.study.domain.trade.model.entity.*;
import com.hjs.study.domain.trade.service.refund.filter.DataNodeFilter;
import com.hjs.study.domain.trade.service.refund.filter.RefundOrderNodeFilter;
import com.hjs.study.domain.trade.service.refund.filter.UniqueRefundNodeFilter;
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
 * 退款责任链工厂。
 * <p>
 * 它把退款入口需要经历的几个步骤装配成责任链：
 * 1. 数据加载节点，准备订单与团队上下文。
 * 2. 重复退款检查节点，保证幂等。
 * 3. 退款执行节点，根据状态选择具体退款策略。
 *
 * @author xiaofuge bugstack.cn @小傅哥
 * 2025/7/30 09:58
 */
@Slf4j
@Service
public class TradeRefundRuleFilterFactory {

    @Bean("tradeRefundRuleFilter")
    public BusinessLinkedList<TradeRefundCommandEntity, DynamicContext, TradeRefundBehaviorEntity> tradeRefundRuleFilter(
            DataNodeFilter dataNodeFilter,
            UniqueRefundNodeFilter uniqueRefundNodeFilter,
            RefundOrderNodeFilter refundOrderNodeFilter) {

        // 退款链路先准备数据，再做幂等，最后才真正执行业务回滚。
        LinkArmory<TradeRefundCommandEntity, TradeRefundRuleFilterFactory.DynamicContext, TradeRefundBehaviorEntity> linkArmory =
                new LinkArmory<>("退单规则过滤链",
                        dataNodeFilter,
                        uniqueRefundNodeFilter,
                        refundOrderNodeFilter);

        // 产出可注入的责任链 Bean。
        return linkArmory.getLogicLink();
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DynamicContext {

        /** 根据外部单号查询到的预购订单实体。 */
        private MarketPayOrderEntity marketPayOrderEntity;

        /** 订单所属的拼团队伍实体。 */
        private GroupBuyTeamEntity groupBuyTeamEntity;

    }

}
