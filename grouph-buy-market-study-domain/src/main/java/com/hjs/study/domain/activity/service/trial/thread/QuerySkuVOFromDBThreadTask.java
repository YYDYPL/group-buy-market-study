package com.hjs.study.domain.activity.service.trial.thread;


import com.hjs.study.domain.activity.adapter.repository.IActivityRepository;
import com.hjs.study.domain.activity.model.valobj.SkuVO;

import java.util.concurrent.Callable;

/**
 * MarketNode 使用的异步任务，用于根据商品 ID 查询 SKU 信息。
 */
public class QuerySkuVOFromDBThreadTask implements Callable<SkuVO> {

    private final String goodsId;

    private final IActivityRepository activityRepository;

    public QuerySkuVOFromDBThreadTask(String goodsId, IActivityRepository activityRepository) {
        this.goodsId = goodsId;
        this.activityRepository = activityRepository;
    }

    /**
     * 通过领域仓储接口读取 SKU 值对象。
     */
    @Override
    public SkuVO call() throws Exception {
        return activityRepository.querySkuByGoodsId(goodsId);
    }
}
