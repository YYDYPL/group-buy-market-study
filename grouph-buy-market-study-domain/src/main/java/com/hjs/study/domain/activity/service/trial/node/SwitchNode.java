package com.hjs.study.domain.activity.service.trial.node;


import com.alibaba.fastjson.JSON;
import com.hjs.study.domain.activity.model.entity.MarketProductEntity;
import com.hjs.study.domain.activity.model.entity.TrialBalanceEntity;
import com.hjs.study.domain.activity.service.trial.AbstractGroupBuyMarketSupport;
import com.hjs.study.domain.activity.service.trial.factory.DefaultActivityStrategyFactory;
import com.hjs.study.types.design.framework.tree.StrategyHandler;
import com.hjs.study.types.enums.ResponseCode;
import com.hjs.study.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 开关控制节点。
 * <p>
 * 该节点负责执行运行时治理逻辑：
 * 1. 判断活动域是否已被整体降级；
 * 2. 判断当前用户是否命中灰度切量范围。
 * 只有通过这两层控制后，才会真正进入营销试算阶段。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 开关节点
 * @create 2024-12-14 14:27
 */
@Slf4j
@Service
public class SwitchNode extends AbstractGroupBuyMarketSupport<MarketProductEntity, DefaultActivityStrategyFactory.DynamicContext, TrialBalanceEntity> {

    /** 下一跳节点，负责真正查询活动配置和执行折扣试算。 */
    @Resource
    private MarketNode marketNode;

    @Override
    /**
     * 执行降级与切量判断。
     *
     * @param requestParameter 试算输入参数
     * @param dynamicContext 流程动态上下文
     * @return 试算结果
     * @throws Exception 降级或未命中切量时抛出业务异常
     */
    public TrialBalanceEntity doApply(MarketProductEntity requestParameter, DefaultActivityStrategyFactory.DynamicContext dynamicContext) throws Exception {
        log.info("拼团商品查询试算服务-SwitchNode userId:{} requestParameter:{}", requestParameter.getUserId(), JSON.toJSONString(requestParameter));

        // 根据用户ID切量
        String userId = requestParameter.getUserId();

        // 判断是否降级
        if (repository.downgradeSwitch()) {
            log.info("拼团活动降级拦截 {}", userId);
            throw new AppException(ResponseCode.E0003.getCode(), ResponseCode.E0003.getInfo());
        }

        // 切量范围判断
        if (!repository.cutRange(userId)) {
            log.info("拼团活动切量拦截 {}", userId);
            throw new AppException(ResponseCode.E0004.getCode(), ResponseCode.E0004.getInfo());
        }

        return router(requestParameter, dynamicContext);
    }

    @Override
    /** 通过治理检查后，继续进入营销节点。 */
    public StrategyHandler<MarketProductEntity, DefaultActivityStrategyFactory.DynamicContext, TrialBalanceEntity> get(MarketProductEntity requestParameter, DefaultActivityStrategyFactory.DynamicContext dynamicContext) throws Exception {
        return marketNode;
    }

}
