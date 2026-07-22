package com.hjs.study.domain.trade.service.lock.factory;

import com.hjs.study.domain.trade.model.entity.GroupBuyActivityEntity;
import com.hjs.study.domain.trade.model.entity.TradeLockRuleCommandEntity;
import com.hjs.study.domain.trade.model.entity.TradeLockRuleFilterBackEntity;
import com.hjs.study.domain.trade.service.lock.filter.ActivityUsabilityRuleFilter;
import com.hjs.study.domain.trade.service.lock.filter.TeamStockOccupyRuleFilter;
import com.hjs.study.domain.trade.service.lock.filter.UserTakeLimitRuleFilter;
import com.hjs.study.types.design.framework.link.model2.LinkArmory;
import com.hjs.study.types.design.framework.link.model2.chain.BusinessLinkedList;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

/**
 * 锁单规则责任链工厂。
 * <p>
 * 它的职责不是执行业务，而是把锁单阶段需要的一组规则节点按顺序装配成责任链：
 * 1. 活动可用性校验。
 * 2. 用户参与次数校验。
 * 3. 团队库存占用校验。
 * <p>
 * 这样做可以把“规则定义顺序”和“规则执行逻辑”分离开，后续扩展节点也更容易。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 交易规则过滤工厂
 * @create 2025-01-25 08:41
 */
@Slf4j
@Service
public class TradeLockRuleFilterFactory {

    /** 团队库存缓存 Key 前缀，用于构造 Redis 侧的团队名额占用标识。 */
    private static final String teamStockKey = "group_buy_market_team_stock_key_";

    @Bean("tradeRuleFilter")
    public BusinessLinkedList<TradeLockRuleCommandEntity, DynamicContext, TradeLockRuleFilterBackEntity> tradeRuleFilter(
            ActivityUsabilityRuleFilter activityUsabilityRuleFilter,
            UserTakeLimitRuleFilter userTakeLimitRuleFilter,
            TeamStockOccupyRuleFilter teamStockOccupyRuleFilter) {

        // 责任链顺序很重要：先校验活动，再校验用户次数，最后再去抢占库存。
        LinkArmory<TradeLockRuleCommandEntity, DynamicContext, TradeLockRuleFilterBackEntity> linkArmory =
                new LinkArmory<>("交易规则过滤链",
                        activityUsabilityRuleFilter,
                        userTakeLimitRuleFilter,
                        teamStockOccupyRuleFilter);

        // 返回可注入 Spring 的链对象 Bean，供锁单服务直接调用。
        return linkArmory.getLogicLink();
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DynamicContext {

        /** 当前命中的拼团活动实体，由前置节点写入，供后续节点复用。 */
        private GroupBuyActivityEntity groupBuyActivity;

        /** 用户已参与次数，由参与次数校验节点查询后写入。 */
        private Integer userTakeOrderCount;

        /**
         * 生成团队库存占用 Key。
         * <p>
         * 只有加入已有团队时才需要占用团队名额，新开团场景返回空即可。
         */
        public String generateTeamStockKey(String teamId) {
            if (StringUtils.isBlank(teamId)) return null;
            return TradeLockRuleFilterFactory.generateTeamStockKey(groupBuyActivity.getActivityId(), teamId);
        }

        /**
         * 生成团队库存恢复 Key。
         * <p>
         * 当后续发生锁单失败、超时未支付或退款回滚时，可以通过该 Key 恢复预占名额。
         */
        public String generateRecoveryTeamStockKey(String teamId) {
            if (StringUtils.isBlank(teamId)) return null;
            return TradeLockRuleFilterFactory.generateRecoveryTeamStockKey(groupBuyActivity.getActivityId(), teamId);
        }

    }

    /** 生成团队库存占用 Key。 */
    public static String generateTeamStockKey(Long activityId, String teamId){
        return teamStockKey + activityId + "_" + teamId;
    }

    /** 生成团队库存恢复 Key。 */
    public static String generateRecoveryTeamStockKey(Long activityId, String teamId) {
        return teamStockKey + activityId + "_" + teamId + "_recovery";
    }

}
