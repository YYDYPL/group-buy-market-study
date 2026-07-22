package com.hjs.study.domain.trade.service.lock.filter;

import com.hjs.study.domain.trade.adapter.repository.ITradeRepository;
import com.hjs.study.domain.trade.model.entity.GroupBuyActivityEntity;
import com.hjs.study.domain.trade.model.entity.TradeLockRuleCommandEntity;
import com.hjs.study.domain.trade.model.entity.TradeLockRuleFilterBackEntity;
import com.hjs.study.domain.trade.service.lock.factory.TradeLockRuleFilterFactory;
import com.hjs.study.types.design.framework.link.model2.handler.ILogicHandler;
import com.hjs.study.types.enums.ActivityStatusEnumVO;
import com.hjs.study.types.enums.ResponseCode;
import com.hjs.study.types.exception.AppException;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;

/**
 * 活动可用性校验节点。
 * <p>
 * 这是锁单责任链的第一道关卡，用于确认活动本身是否还允许参与。
 * 如果活动都不可用，就没有必要继续往下做次数校验或库存占用，
 * 因此它被放在最前面做快速失败。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 活动的可用性，规则过滤【状态、有效期】
 * @create 2025-01-25 09:18
 */
@Slf4j
@Service
public class ActivityUsabilityRuleFilter implements ILogicHandler<TradeLockRuleCommandEntity, TradeLockRuleFilterFactory.DynamicContext, TradeLockRuleFilterBackEntity> {

    /** 交易仓储，用于查询活动快照。 */
    @Resource
    private ITradeRepository repository;

    @Override
    public TradeLockRuleFilterBackEntity apply(TradeLockRuleCommandEntity requestParameter, TradeLockRuleFilterFactory.DynamicContext dynamicContext) throws Exception {
        log.info("交易规则过滤-活动的可用性校验{} activityId:{}", requestParameter.getUserId(), requestParameter.getActivityId());

        // 先查出活动快照，后续多个规则节点都可能依赖它。
        GroupBuyActivityEntity groupBuyActivity = repository.queryGroupBuyActivityEntityByActivityId(requestParameter.getActivityId());

        // 活动必须处于“生效中”，否则说明还没开始、已过期或已废弃。
        if (!ActivityStatusEnumVO.EFFECTIVE.equals(groupBuyActivity.getStatus())) {
            log.info("活动的可用性校验，非生效状态 activityId:{}", requestParameter.getActivityId());
            throw new AppException(ResponseCode.E0101);
        }

        // 还要确保当前时间落在活动可参与窗口内。
        Date currentTime = new Date();
        if (currentTime.before(groupBuyActivity.getStartTime()) || currentTime.after(groupBuyActivity.getEndTime())) {
            log.info("活动的可用性校验，非可参与时间范围 activityId:{}", requestParameter.getActivityId());
            throw new AppException(ResponseCode.E0102);
        }

        // 活动快照写入动态上下文，供后续用户限次和库存节点直接复用。
        dynamicContext.setGroupBuyActivity(groupBuyActivity);

        // 当前节点通过后，才有资格进入下一个规则节点。
        return next(requestParameter, dynamicContext);
    }
}
