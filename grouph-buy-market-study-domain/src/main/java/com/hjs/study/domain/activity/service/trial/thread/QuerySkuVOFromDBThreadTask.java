package com.hjs.study.domain.activity.service.trial.thread;


import com.hjs.study.domain.activity.adapter.repository.IActivityRepository;
import com.hjs.study.domain.activity.model.valobj.SkuVO;

import java.util.concurrent.Callable;

/**
 * 异步查询商品信息任务。
 * <p>
 * 当前示例直接从仓储查询商品信息，
 * 在真实生产场景中也可以替换成 RPC、商品中心缓存或同步库查询。
 * 把它单独封装成 `Callable` 后，营销节点就能并行拉取商品数据。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 查询商品信息任务
 * @create 2024-12-21 10:51
 */
public class QuerySkuVOFromDBThreadTask implements Callable<SkuVO> {

    /** 商品 ID。 */
    private final String goodsId;

    /** 活动域仓储，用于查询商品基础信息。 */
    private final IActivityRepository activityRepository;

    public QuerySkuVOFromDBThreadTask(String goodsId, IActivityRepository activityRepository) {
        this.goodsId = goodsId;
        this.activityRepository = activityRepository;
    }

    @Override
    /** 执行商品信息查询。 */
    public SkuVO call() throws Exception {
        return activityRepository.querySkuByGoodsId(goodsId);
    }

}
