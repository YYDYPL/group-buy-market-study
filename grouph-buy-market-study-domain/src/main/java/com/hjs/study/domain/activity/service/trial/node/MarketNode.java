package com.hjs.study.domain.activity.service.trial.node;

import com.alibaba.fastjson.JSON;
import com.hjs.study.domain.activity.model.entity.MarketProductEntity;
import com.hjs.study.domain.activity.model.entity.TrialBalanceEntity;
import com.hjs.study.domain.activity.model.valobj.GroupBuyActivityDiscountVO;
import com.hjs.study.domain.activity.model.valobj.SkuVO;
import com.hjs.study.domain.activity.service.discount.IDiscountCalculateService;
import com.hjs.study.domain.activity.service.trial.AbstractGroupBuyMarketSupport;
import com.hjs.study.domain.activity.service.trial.factory.DefaultActivityStrategyFactory;
import com.hjs.study.domain.activity.service.trial.thread.QueryGroupBuyActivityDiscountVOThreadTask;
import com.hjs.study.domain.activity.service.trial.thread.QuerySkuVOFromDBThreadTask;
import com.hjs.study.types.design.framework.tree.StrategyHandler;
import com.hjs.study.types.enums.ResponseCode;
import com.hjs.study.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.*;

@Service
@Slf4j
public class MarketNode extends AbstractGroupBuyMarketSupport<MarketProductEntity, DefaultActivityStrategyFactory.DynamicContext, TrialBalanceEntity> {

    /**
     * 共享线程池，用于并行加载互不依赖的试算数据。
     */
    @Resource
    private ThreadPoolExecutor threadPoolExecutor;
    @Resource
    private EndNode endNode;
    @Resource
    private Map<String, IDiscountCalculateService> discountCalculateServiceMap;

    /**
     * 并行预加载活动折扣配置和商品信息。
     * 查询结果写入动态上下文，供后续价格计算使用。
     */
    @Override
    protected void multiThread(MarketProductEntity requestParameter, DefaultActivityStrategyFactory.DynamicContext dynamicContext) throws ExecutionException, InterruptedException, TimeoutException {
        //异步查询活动配置
        QueryGroupBuyActivityDiscountVOThreadTask queryGroupBuyActivityDiscountVOThreadTask = new QueryGroupBuyActivityDiscountVOThreadTask(requestParameter.getSource(), requestParameter.getChannel(), repository);
        FutureTask<GroupBuyActivityDiscountVO> groupBuyActivityDiscountVOFutureTask = new FutureTask<>(queryGroupBuyActivityDiscountVOThreadTask);
        threadPoolExecutor.execute(groupBuyActivityDiscountVOFutureTask);

        //异步查询商品信息
        QuerySkuVOFromDBThreadTask querySkuVOFromDBThreadTask = new QuerySkuVOFromDBThreadTask(requestParameter.getGoodsId(), repository);
        FutureTask<SkuVO> skuVOFutureTask = new FutureTask<>(querySkuVOFromDBThreadTask);
        threadPoolExecutor.execute(skuVOFutureTask);

        //写入上下文
        dynamicContext.setGroupBuyActivityDiscountVO(groupBuyActivityDiscountVOFutureTask.get(timeout, TimeUnit.MINUTES));
        dynamicContext.setSkuVO(skuVOFutureTask.get(timeout,TimeUnit.MINUTES));

        log.info("拼团商品查询试算服务-MarketNode userId:{} 异步线程加载数据「GroupBuyActivityDiscountVO、SkuVO」完成", requestParameter.getUserId());


    }

    /**
     * 根据营销计划选择折扣策略，并计算本次试算的应付价格。
     */
    @Override
    protected TrialBalanceEntity doApply(MarketProductEntity requestParameter, DefaultActivityStrategyFactory.DynamicContext dynamicContext) throws Exception {
        log.info("拼团商品查询试算服务-MarketNode userId:{} requestParameter:{}", requestParameter.getUserId(), JSON.toJSONString(requestParameter));
        // todo 拼团优惠试算
        GroupBuyActivityDiscountVO groupBuyActivityDiscountVO = dynamicContext.getGroupBuyActivityDiscountVO();
        GroupBuyActivityDiscountVO.GroupBuyDiscount groupBuyDiscount = groupBuyActivityDiscountVO.getGroupBuyDiscount();

        SkuVO skuVO = dynamicContext.getSkuVO();

        IDiscountCalculateService discountCalculateService = discountCalculateServiceMap.get(groupBuyDiscount.getMarketPlan());
        if (null==discountCalculateService){
            log.info("不存在{}类型的折扣计算服务，支持类型为:{}", groupBuyDiscount.getMarketPlan(), JSON.toJSONString(discountCalculateServiceMap.keySet()));
            throw new AppException(ResponseCode.E0001.getCode(),ResponseCode.E0001.getInfo());
        }

        BigDecimal deductionPrice = discountCalculateService.calculate(requestParameter.getUserId(), skuVO.getOriginalPrice(), groupBuyDiscount);
        dynamicContext.setDeductionPrice(deductionPrice);


        return router(requestParameter,dynamicContext);
    }

    /**
     * 价格计算完成后，将已填充的上下文交给结果组装节点。
     */
    @Override
    public StrategyHandler<MarketProductEntity, DefaultActivityStrategyFactory.DynamicContext, TrialBalanceEntity> get(MarketProductEntity requestParameter, DefaultActivityStrategyFactory.DynamicContext dynamicContext) throws Exception {
        return endNode;
    }
}
