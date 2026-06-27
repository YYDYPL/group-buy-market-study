package com.hjs.study.domain.activity.service.trial.node;


import com.alibaba.fastjson.JSON;
import com.hjs.study.domain.activity.model.entity.MarketProductEntity;
import com.hjs.study.domain.activity.model.entity.TrialBalanceEntity;
import com.hjs.study.domain.activity.model.valobj.GroupBuyActivityDiscountVO;
import com.hjs.study.domain.activity.model.valobj.SkuVO;
import com.hjs.study.domain.activity.service.trial.AbstractGroupBuyMarketSupport;
import com.hjs.study.domain.activity.service.trial.factory.DefaultActivityStrategyFactory;
import com.hjs.study.types.design.framework.tree.StrategyHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
public class EndNode extends AbstractGroupBuyMarketSupport<MarketProductEntity, DefaultActivityStrategyFactory.DynamicContext, TrialBalanceEntity> {
    /**
     * 结束节点：根据上下文中累计的数据组装试算结果。
     */
    @Override
    protected TrialBalanceEntity doApply(MarketProductEntity requestParameter, DefaultActivityStrategyFactory.DynamicContext dynamicContext) throws Exception {
        log.info("拼团商品查询试算服务-EndNode userId:{} requestParameter:{}", requestParameter.getUserId(), JSON.toJSONString(requestParameter));

        GroupBuyActivityDiscountVO groupBuyActivityDiscountVO = dynamicContext.getGroupBuyActivityDiscountVO();
        SkuVO skuVO = dynamicContext.getSkuVO();
        BigDecimal deductionPrice = dynamicContext.getDeductionPrice();

        return TrialBalanceEntity.builder()
                .goodsId(skuVO.getGoodsId())
                .goodsName(skuVO.getGoodsName())
                .originalPrice(skuVO.getOriginalPrice())
                .deductionPrice(deductionPrice)
                .targetCount(groupBuyActivityDiscountVO.getTarget())
                .startTime(groupBuyActivityDiscountVO.getStartTime())
                .endTime(groupBuyActivityDiscountVO.getEndTime())
                .isVisible(false)
                .isEnable(false)
                .build();
    }

    /**
     * 结果实体构建完成后结束责任树。
     */
    @Override
    public StrategyHandler<MarketProductEntity, DefaultActivityStrategyFactory.DynamicContext, TrialBalanceEntity> get(MarketProductEntity requestParameter, DefaultActivityStrategyFactory.DynamicContext dynamicContext) throws Exception {
        return defaultStrategyHandler;
    }
}
