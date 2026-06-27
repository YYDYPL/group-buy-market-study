package com.hjs.study.domain.activity.service.trial;

import com.hjs.study.domain.activity.adapter.repository.IActivityRepository;
import com.hjs.study.domain.activity.model.entity.MarketProductEntity;
import com.hjs.study.domain.activity.model.entity.TrialBalanceEntity;
import com.hjs.study.domain.activity.service.trial.factory.DefaultActivityStrategyFactory;
import com.hjs.study.types.design.framework.tree.AbstractMultiThreadStrategyRouter;

import javax.annotation.Resource;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * 拼团试算策略节点公共支撑类。
 * 节点需要在路由前预加载数据时，可以覆写 {@link #multiThread}。
 */
public abstract class AbstractGroupBuyMarketSupport<MarketProductEntity, DynamicContext, TrialBalanceEntity> extends AbstractMultiThreadStrategyRouter<com.hjs.study.domain.activity.model.entity.MarketProductEntity, DefaultActivityStrategyFactory.DynamicContext, com.hjs.study.domain.activity.model.entity.TrialBalanceEntity> {

    /**
     * 节点异步查询的超时时间。
     */
    protected long timeout = 500;
    @Resource
    protected IActivityRepository repository;

    @Override
    protected void multiThread(com.hjs.study.domain.activity.model.entity.MarketProductEntity requestParameter, DefaultActivityStrategyFactory.DynamicContext dynamicContext) throws ExecutionException, InterruptedException, TimeoutException {
        // 缺省的方法
    }

}
