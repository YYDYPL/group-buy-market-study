package com.hjs.study.domain.activity.service.trial.node;

import com.hjs.study.domain.activity.model.entity.MarketProductEntity;
import com.hjs.study.domain.activity.model.valobj.GroupBuyActivityDiscountVO;
import com.hjs.study.domain.activity.model.valobj.SCSkuActivityVO;
import com.hjs.study.domain.activity.model.valobj.SkuVO;
import com.hjs.study.domain.activity.service.trial.factory.DefaultActivityStrategyFactory;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeoutException;

/**
 * 使用 `CompletableFuture` 的营销节点示例实现。
 * <p>
 * 它与 `MarketNode` 的职责相同，区别只在于异步编排方式：
 * `MarketNode` 使用 `FutureTask`；
 * 该类使用 `CompletableFuture`，代码更适合展示现代 Java 的异步风格。
 * 当前默认未启用，主要作为对照样例保留。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 线程案例举例
 * @create 2025-04-03 07:44
 */
@Slf4j
//@Service
public class MarketNode2CompletableFuture extends MarketNode {

    /** 异步任务线程池。 */
    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @Override
    /**
     * 使用 `CompletableFuture` 并行查询活动配置与商品信息。
     *
     * @param requestParameter 试算输入参数
     * @param dynamicContext 流程动态上下文
     */
    protected void multiThread(MarketProductEntity requestParameter, DefaultActivityStrategyFactory.DynamicContext dynamicContext) throws ExecutionException, InterruptedException, TimeoutException {
        // 异步查询活动配置
        CompletableFuture<GroupBuyActivityDiscountVO> groupBuyActivityDiscountVOCompletableFuture = CompletableFuture.supplyAsync(() -> {
            try {
                Long availableActivityId = requestParameter.getActivityId();
                if (null == requestParameter.getActivityId()) {
                    // 查询渠道商品活动配置关联配置
                    SCSkuActivityVO scSkuActivityVO = repository.querySCSkuActivityBySCGoodsId(requestParameter.getSource(), requestParameter.getChannel(), requestParameter.getGoodsId());
                    if (null == scSkuActivityVO) return null;
                    availableActivityId = scSkuActivityVO.getActivityId();
                }
                // 查询活动配置
                return repository.queryGroupBuyActivityDiscountVO(availableActivityId);
            } catch (Exception e) {
                log.error("异步查询活动配置异常", e);
                return null;
            }
        }, threadPoolExecutor);

        // 异步查询商品信息 - 在实际生产中，商品有同步库或者调用接口查询。这里暂时使用DB方式查询。
        CompletableFuture<SkuVO> skuVOCompletableFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return repository.querySkuByGoodsId(requestParameter.getGoodsId());
            } catch (Exception e) {
                log.error("异步查询商品信息异常", e);
                return null;
            }
        }, threadPoolExecutor);

        // 等待所有异步任务完成并写入上下文
        CompletableFuture.allOf(groupBuyActivityDiscountVOCompletableFuture, skuVOCompletableFuture)
                .thenRun(() -> {
                    dynamicContext.setGroupBuyActivityDiscountVO(groupBuyActivityDiscountVOCompletableFuture.join());
                    dynamicContext.setSkuVO(skuVOCompletableFuture.join());
                }).join();

        log.info("拼团商品查询试算服务-MarketNode userId:{} 异步线程加载数据「GroupBuyActivityDiscountVO、SkuVO」完成", requestParameter.getUserId());
    }
}
