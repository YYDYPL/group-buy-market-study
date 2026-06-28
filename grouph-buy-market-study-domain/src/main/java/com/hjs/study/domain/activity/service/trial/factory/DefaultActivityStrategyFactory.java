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

@Service
public class DefaultActivityStrategyFactory {

    /**
     * 试算责任树的根节点。
     */
    private final RootNode rootNode;

    public DefaultActivityStrategyFactory(RootNode rootNode) {
        this.rootNode = rootNode;
    }

    /**
     * 将根节点暴露为通用策略处理器，供应用服务层调用。
     */
    public StrategyHandler<MarketProductEntity,DynamicContext, TrialBalanceEntity> strategyHandler(){
        return rootNode;
    }

    /**
     * 策略树流转过程中的共享上下文。
     * 前置节点写入自己负责的数据，后续节点从同一个上下文读取。
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DynamicContext {
        // 拼团活动营销配置值对象
        private GroupBuyActivityDiscountVO groupBuyActivityDiscountVO;
        // 商品信息
        private SkuVO skuVO;
        // 折扣价格
        private BigDecimal deductionPrice;
        // 活动可见性限制
        private boolean visible;
        // 活动
        private boolean enable;
    }

}
