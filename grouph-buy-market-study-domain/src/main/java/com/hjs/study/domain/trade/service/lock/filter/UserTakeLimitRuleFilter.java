package com.hjs.study.domain.trade.service.lock.filter;

import com.hjs.study.domain.trade.adapter.repository.ITradeRepository;
import com.hjs.study.domain.trade.model.entity.GroupBuyActivityEntity;
import com.hjs.study.domain.trade.model.entity.TradeLockRuleCommandEntity;
import com.hjs.study.domain.trade.model.entity.TradeLockRuleFilterBackEntity;
import com.hjs.study.domain.trade.service.lock.factory.TradeLockRuleFilterFactory;
import com.hjs.study.types.design.framework.link.model2.handler.ILogicHandler;
import com.hjs.study.types.enums.ResponseCode;
import com.hjs.study.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 用户参与次数限制校验节点。
 * <p>
 * 该节点负责判断用户在同一个活动上是否已经达到参与上限，
 * 是防止重复薅活动和滥用优惠的重要业务规则。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 用户参与限制，规则过滤
 * @create 2025-01-25 09:19
 */
@Slf4j
@Service
public class UserTakeLimitRuleFilter implements ILogicHandler<TradeLockRuleCommandEntity, TradeLockRuleFilterFactory.DynamicContext, TradeLockRuleFilterBackEntity> {

    /** 交易仓储，用于统计用户在当前活动下的已参与订单数。 */
    @Resource
    private ITradeRepository repository;

    @Override
    public TradeLockRuleFilterBackEntity apply(TradeLockRuleCommandEntity requestParameter, TradeLockRuleFilterFactory.DynamicContext dynamicContext) throws Exception {
        log.info("交易规则过滤-用户参与次数校验{} activityId:{}", requestParameter.getUserId(), requestParameter.getActivityId());

        // 活动节点已提前把活动快照写入上下文，这里直接取出复用。
        GroupBuyActivityEntity groupBuyActivity = dynamicContext.getGroupBuyActivity();

        // 统计用户在该活动下已经参与过多少笔订单。
        Integer count = repository.queryOrderCountByActivityId(requestParameter.getActivityId(), requestParameter.getUserId());

        // 达到上限则直接终止责任链，防止继续占用团队库存。
        if (null != groupBuyActivity.getTakeLimitCount() && count >= groupBuyActivity.getTakeLimitCount()) {
            log.info("用户参与次数校验，已达可参与上限 activityId:{}", requestParameter.getActivityId());
            throw new AppException(ResponseCode.E0103);
        }

        // 把参与次数放入上下文，后续锁单阶段可直接用于拼装幂等业务号。
        dynamicContext.setUserTakeOrderCount(count);

        // 通过后进入库存占用校验。
        return next(requestParameter, dynamicContext);
    }

}
