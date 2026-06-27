package com.hjs.study.domain.activity.service;

import com.hjs.study.domain.activity.model.entity.MarketProductEntity;
import com.hjs.study.domain.activity.model.entity.TrialBalanceEntity;
import com.hjs.study.domain.activity.service.trial.factory.DefaultActivityStrategyFactory;
import com.hjs.study.types.design.framework.tree.StrategyHandler;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class IIndexGroupBuyMarketServiceImpl implements IIndexGroupBuyMarketService{


    @Resource
    private DefaultActivityStrategyFactory defaultActivityStrategyFactory;

    /**
     * 拼团营销试算入口。
     * 具体业务步骤交给策略工厂构建的责任树执行。
     */
    @Override
    public TrialBalanceEntity indexMarketTrial(MarketProductEntity marketProductEntity) throws Exception {

        StrategyHandler<MarketProductEntity, DefaultActivityStrategyFactory.DynamicContext, TrialBalanceEntity> strategyHandler = defaultActivityStrategyFactory.strategyHandler();

        TrialBalanceEntity trialBalanceEntity = strategyHandler.apply(marketProductEntity, new DefaultActivityStrategyFactory.DynamicContext());

        return trialBalanceEntity;
    }
}
