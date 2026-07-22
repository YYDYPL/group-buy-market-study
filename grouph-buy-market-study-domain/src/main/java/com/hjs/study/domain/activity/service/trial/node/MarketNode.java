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

/**
 * 营销计算节点。
 * <p>
 * 这是试算流程树里最核心的节点之一，负责：
 * 1. 并行预加载活动配置和商品信息；
 * 2. 选择对应的折扣计算策略；
 * 3. 计算优惠金额和最终支付价格；
 * 4. 决定后续进入正常结束节点还是异常节点。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 营销优惠节点
 * @create 2024-12-14 14:30
 */
@Slf4j
@Service
public class MarketNode extends AbstractGroupBuyMarketSupport<MarketProductEntity, DefaultActivityStrategyFactory.DynamicContext, TrialBalanceEntity> {

    /** 线程池，用于并行预加载活动配置和商品信息。 */
    @Resource
    private ThreadPoolExecutor threadPoolExecutor;
    /**
     * <a href="https://bugstack.cn/md/road-map/spring-dependency-injection.html">Spring 注入详细说明</a>
     */
    /** 折扣计算策略集合，key 通常为 `ZJ`、`MJ`、`N`、`ZK` 等计划编码。 */
    @Resource
    private Map<String, IDiscountCalculateService> discountCalculateServiceMap;
    /** 异常节点，用于统一处理无营销配置等异常情况。 */
    @Resource
    private ErrorNode errorNode;
    /** 标签节点，用于进一步判断当前用户的可见性和参与资格。 */
    @Resource
    private TagNode tagNode;

    /**
     * 在 MarketNode2CompletableFuture 继承的子类实现一个 CompletableFuture 多线程方式。
     * <p>
     * 1. CompletableFuture：适用于大多数现代 Java 应用，尤其在需要灵活任务编排时。
     * 2.  FutureTask：任务极度简单，适合简单场景。
     * <p>
     * | 对比维度    | FutureTask             | CompletableFuture                |
     * | :--------------- | :-------------------------- | :------------------------------------- |
     * | 任务编排能力 | 弱（需手动管理多个 Future） | 强（内置 `thenApply`、`allOf` 等方法） |
     * | 代码简洁性  | 冗余（显式调用 `get()`）    | 简洁（链式调用，逻辑内聚）             |
     * | 异常处理   | 繁琐（需捕获多个异常）      | 优雅（支持 `exceptionally` 统一处理）  |
     * | 线程阻塞     | 可能多次阻塞主线程          | 非阻塞或单次阻塞（如 `join()`）        |
     * | 适用场景     | 简单任务、低版本 Java 环境  | 复杂异步流程、Java 8+ 环境             |
     * <p>
     * 使用；MarketNode 的 @Service 注释掉，MarketNode2CompletableFuture 的 @Service 打开，就可以使用了。
     */
    @Override
    protected void multiThread(MarketProductEntity requestParameter, DefaultActivityStrategyFactory.DynamicContext dynamicContext) throws ExecutionException, InterruptedException, TimeoutException {
        // 异步查询活动配置
        QueryGroupBuyActivityDiscountVOThreadTask queryGroupBuyActivityDiscountVOThreadTask = new QueryGroupBuyActivityDiscountVOThreadTask(requestParameter.getActivityId(), requestParameter.getSource(), requestParameter.getChannel(), requestParameter.getGoodsId(), repository);
        FutureTask<GroupBuyActivityDiscountVO> groupBuyActivityDiscountVOFutureTask = new FutureTask<>(queryGroupBuyActivityDiscountVOThreadTask);
        threadPoolExecutor.execute(groupBuyActivityDiscountVOFutureTask);

        // 异步查询商品信息 - 在实际生产中，商品有同步库或者调用接口查询。这里暂时使用DB方式查询。
        QuerySkuVOFromDBThreadTask querySkuVOFromDBThreadTask = new QuerySkuVOFromDBThreadTask(requestParameter.getGoodsId(), repository);
        FutureTask<SkuVO> skuVOFutureTask = new FutureTask<>(querySkuVOFromDBThreadTask);
        threadPoolExecutor.execute(skuVOFutureTask);

        // 写入上下文 - 让后续节点直接消费这些数据，而不用重复查询。
        dynamicContext.setGroupBuyActivityDiscountVO(groupBuyActivityDiscountVOFutureTask.get(timeout, TimeUnit.MILLISECONDS));
        dynamicContext.setSkuVO(skuVOFutureTask.get(timeout, TimeUnit.MILLISECONDS));

        log.info("拼团商品查询试算服务-MarketNode userId:{} 异步线程加载数据「GroupBuyActivityDiscountVO、SkuVO」完成", requestParameter.getUserId());
    }

    @Override
    /**
     * 执行活动试算核心逻辑。
     *
     * @param requestParameter 试算输入参数
     * @param dynamicContext 流程动态上下文
     * @return 试算结果
     * @throws Exception 折扣策略缺失或下游异常
     */
    public TrialBalanceEntity doApply(MarketProductEntity requestParameter, DefaultActivityStrategyFactory.DynamicContext dynamicContext) throws Exception {
        log.info("拼团商品查询试算服务-MarketNode userId:{} requestParameter:{}", requestParameter.getUserId(), JSON.toJSONString(requestParameter));

        // 获取上下文数据
        GroupBuyActivityDiscountVO groupBuyActivityDiscountVO = dynamicContext.getGroupBuyActivityDiscountVO();
        if (null == groupBuyActivityDiscountVO) {
            return router(requestParameter, dynamicContext);
        }

        GroupBuyActivityDiscountVO.GroupBuyDiscount groupBuyDiscount = groupBuyActivityDiscountVO.getGroupBuyDiscount();
        SkuVO skuVO = dynamicContext.getSkuVO();
        if (null == groupBuyDiscount || null == skuVO) {
            return router(requestParameter, dynamicContext);
        }

        // 优惠试算
        IDiscountCalculateService discountCalculateService = discountCalculateServiceMap.get(groupBuyDiscount.getMarketPlan());
        if (null == discountCalculateService) {
            log.info("不存在{}类型的折扣计算服务，支持类型为:{}", groupBuyDiscount.getMarketPlan(), JSON.toJSONString(discountCalculateServiceMap.keySet()));
            throw new AppException(ResponseCode.E0001.getCode(), ResponseCode.E0001.getInfo());
        }

        // 计算最终支付金额，并顺带得到优惠金额。
        BigDecimal payPrice = discountCalculateService.calculate(requestParameter.getUserId(), skuVO.getOriginalPrice(), groupBuyDiscount);
        dynamicContext.setDeductionPrice(skuVO.getOriginalPrice().subtract(payPrice));
        dynamicContext.setPayPrice(payPrice);

        return router(requestParameter, dynamicContext);
    }

    @Override
    /**
     * 根据上下文结果决定下一跳。
     * <p>
     * 如果活动、商品或优惠金额缺失，说明当前请求无法完成有效试算，应进入异常节点；
     * 否则继续进入标签判断节点。
     */
    public StrategyHandler<MarketProductEntity, DefaultActivityStrategyFactory.DynamicContext, TrialBalanceEntity> get(MarketProductEntity requestParameter, DefaultActivityStrategyFactory.DynamicContext dynamicContext) throws Exception {
        // 不存在配置的拼团活动，走异常节点
        if (null == dynamicContext.getGroupBuyActivityDiscountVO() || null == dynamicContext.getSkuVO() || null == dynamicContext.getDeductionPrice()) {
            return errorNode;
        }

        return tagNode;
    }

}
