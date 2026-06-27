package com.hjs.study.domain.activity.service.trial.thread;

import com.hjs.study.domain.activity.adapter.repository.IActivityRepository;
import com.hjs.study.domain.activity.model.valobj.GroupBuyActivityDiscountVO;

import java.util.concurrent.Callable;

/**
 * MarketNode 使用的异步任务，用于查询活动与折扣配置。
 */
public class QueryGroupBuyActivityDiscountVOThreadTask implements Callable<GroupBuyActivityDiscountVO> {

    private final String source;

    private final String channel;

    private final IActivityRepository activityRepository;

    public QueryGroupBuyActivityDiscountVOThreadTask(String source, String channel,IActivityRepository activityRepository) {
        this.source = source;
        this.channel = channel;
        this.activityRepository = activityRepository;
    }

    /**
     * 通过仓储接口查询数据，使领域任务不依赖具体持久化实现。
     */
    @Override
    public GroupBuyActivityDiscountVO call() throws Exception {
        return activityRepository.queryGroupBuyActivityDiscountVO(source, channel);
    }
}
