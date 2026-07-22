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
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 活动试算根节点。
 * <p>
 * 它是整个策略树的第一个节点，主要负责做最基础的入参校验，
 * 校验通过后再把请求继续路由给下一个节点。
 * 可以把它理解成整条试算链的“入口门卫”。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 根节点
 * @create 2024-12-14 14:17
 */
@Slf4j
@Service
public class RootNode extends AbstractGroupBuyMarketSupport<MarketProductEntity, DefaultActivityStrategyFactory.DynamicContext, TrialBalanceEntity> {

    /** 下一跳节点，负责处理开关、切量和降级逻辑。 */
    @Resource
    private SwitchNode switchNode;

    @Override
    /**
     * 执行根节点逻辑：参数校验并继续路由。
     *
     * @param requestParameter 试算输入参数
     * @param dynamicContext 流程动态上下文
     * @return 试算结果
     * @throws Exception 参数非法或下游节点异常
     */
    protected TrialBalanceEntity doApply(MarketProductEntity requestParameter, DefaultActivityStrategyFactory.DynamicContext dynamicContext) throws Exception {
        log.info("拼团商品查询试算服务-RootNode userId:{} requestParameter:{}", requestParameter.getUserId(), JSON.toJSONString(requestParameter));
        // 参数判断
        if (StringUtils.isBlank(requestParameter.getUserId()) || StringUtils.isBlank(requestParameter.getGoodsId()) ||
                StringUtils.isBlank(requestParameter.getSource()) || StringUtils.isBlank(requestParameter.getChannel())) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), ResponseCode.ILLEGAL_PARAMETER.getInfo());
        }
        return router(requestParameter, dynamicContext);
    }

    @Override
    /** 根节点固定路由到开关节点。 */
    public StrategyHandler<MarketProductEntity, DefaultActivityStrategyFactory.DynamicContext, TrialBalanceEntity> get(MarketProductEntity requestParameter, DefaultActivityStrategyFactory.DynamicContext dynamicContext) throws Exception {
        return switchNode;
    }

}
