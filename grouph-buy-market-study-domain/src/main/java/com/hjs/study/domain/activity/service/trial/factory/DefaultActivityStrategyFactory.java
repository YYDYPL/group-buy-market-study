package com.hjs.study.domain.activity.service.trial.factory;


import com.hjs.study.domain.activity.model.entity.MarketProductEntity;
import com.hjs.study.domain.activity.model.entity.TrialBalanceEntity;
import com.hjs.study.domain.activity.model.valobj.GroupBuyActivityDiscountVO;
import com.hjs.study.domain.activity.model.valobj.SkuVO;
import com.hjs.study.domain.activity.service.trial.node.RootNode;
import com.hjs.study.types.design.framework.tree.StrategyHandler;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 默认活动试算策略工厂。
 * <p>
 * 该工厂的职责并不是创建很多不同实现，而是对外统一暴露活动试算流程树的入口。
 * 调用方只需要拿到一个 `StrategyHandler` 即可开始试算，
 * 无需知道底层由 RootNode、SwitchNode、MarketNode、TagNode 等多个节点组成。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 活动策略工厂
 * @create 2024-12-14 13:41
 */
@Service
public class DefaultActivityStrategyFactory {

    /** 试算流程树根节点，是整个活动试算链路的统一入口。 */
    private final RootNode rootNode;

    public DefaultActivityStrategyFactory(RootNode rootNode) {
        this.rootNode = rootNode;
    }

    /**
     * 返回试算流程树入口。
     *
     * @return 根策略处理器
     */
    public StrategyHandler<MarketProductEntity, DynamicContext, TrialBalanceEntity> strategyHandler() {
        return rootNode;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    /**
     * 试算链路动态上下文。
     * <p>
     * 各节点会把中间产物逐步写入这里，后续节点继续读取并补充，
     * 这样可以避免方法参数层层透传，也能体现“流程中间态”的概念。
     */
    public static class DynamicContext {
        /** 活动与折扣组合配置。 */
        private GroupBuyActivityDiscountVO groupBuyActivityDiscountVO;
        /** 商品基础信息。 */
        private SkuVO skuVO;
        /** 优惠金额。 */
        private BigDecimal deductionPrice;
        /** 最终支付金额。 */
        private BigDecimal payPrice;
        /** 当前用户是否可见该活动。 */
        private boolean visible;
        /** 当前用户是否允许参与该活动。 */
        private boolean enable;
    }

}
