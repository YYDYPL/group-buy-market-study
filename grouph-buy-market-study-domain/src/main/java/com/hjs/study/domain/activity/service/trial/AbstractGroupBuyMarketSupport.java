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
 * 活动试算流程树公共支撑基类。
 * <p>
 * 所有试算节点都继承自该类，从而共享：
 * 1. 活动域仓储访问能力；
 * 2. 多线程预加载扩展点；
 * 3. 统一的策略树父类能力。
 * 这让具体节点只需要专注自己那一段业务职责。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 抽象的拼团营销支撑类
 * @create 2024-12-14 13:42
 */
public abstract class AbstractGroupBuyMarketSupport<MarketProductEntity, DynamicContext, TrialBalanceEntity> extends AbstractMultiThreadStrategyRouter<com.hjs.study.domain.activity.model.entity.MarketProductEntity, DefaultActivityStrategyFactory.DynamicContext, com.hjs.study.domain.activity.model.entity.TrialBalanceEntity> {

    /** 异步查询活动和商品配置时的超时时间，单位毫秒。 */
    protected long timeout = 500;
    /** 活动域仓储，用于节点查询活动配置、商品信息和标签命中结果。 */
    @Resource
    protected IActivityRepository repository;

    @Override
    /**
     * 多线程预处理扩展点。
     * <p>
     * 默认实现为空，具体节点可以在进入主逻辑前并行预取所需数据。
     */
    protected void multiThread(com.hjs.study.domain.activity.model.entity.MarketProductEntity requestParameter, DefaultActivityStrategyFactory.DynamicContext dynamicContext) throws ExecutionException, InterruptedException, TimeoutException {
        // 缺省的方法
    }

}
