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
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 团队库存占用校验节点。
 * <p>
 * 该节点专门处理“加入已有团队”时的名额占用问题。
 * 新开团没有现成团队可抢占，因此无需检查团队库存；
 * 只有参团时才需要先在 Redis 里占位，避免多人并发把团队目标人数冲穿。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 组队库存占用规则过滤
 * @create 2025-04-05 09:41
 */
@Slf4j
@Service
public class TeamStockOccupyRuleFilter implements ILogicHandler<TradeLockRuleCommandEntity, TradeLockRuleFilterFactory.DynamicContext, TradeLockRuleFilterBackEntity> {

    /** 交易仓储，负责实际调用库存占用与恢复逻辑。 */
    @Resource
    private ITradeRepository repository;

    @Override
    public TradeLockRuleFilterBackEntity apply(TradeLockRuleCommandEntity requestParameter, TradeLockRuleFilterFactory.DynamicContext dynamicContext) throws Exception {
        log.info("交易规则过滤-组队库存校验{} activityId:{}", requestParameter.getUserId(), requestParameter.getActivityId());

        // 开新团时没有已有团队可抢占名额，因此直接放行。
        String teamId = requestParameter.getTeamId();
        if (StringUtils.isBlank(teamId)) {
            return TradeLockRuleFilterBackEntity.builder()
                    .userTakeOrderCount(dynamicContext.getUserTakeOrderCount())
                    .build();
        }

        // 参团时需要先抢占团队库存。这里走缓存占位，尽量避免高并发直接打数据库。
        GroupBuyActivityEntity groupBuyActivity = dynamicContext.getGroupBuyActivity();
        Integer target = groupBuyActivity.getTarget();
        Integer validTime = groupBuyActivity.getValidTime();
        String teamStockKey = dynamicContext.generateTeamStockKey(teamId);
        String recoveryTeamStockKey = dynamicContext.generateRecoveryTeamStockKey(teamId);

        boolean status = repository.occupyTeamStock(teamStockKey, recoveryTeamStockKey, target, validTime);

        // 占位失败通常意味着团队已满、已失效或并发下名额已被其他请求先抢走。
        if (!status) {
            log.warn("交易规则过滤-组队库存校验{} activityId:{} 抢占失败:{}", requestParameter.getUserId(), requestParameter.getActivityId(), teamStockKey);
            throw new AppException(ResponseCode.E0008);
        }

        // 把参与次数和恢复库存 Key 返回给后续锁单服务，便于失败时回补。
        return TradeLockRuleFilterBackEntity.builder()
                .userTakeOrderCount(dynamicContext.getUserTakeOrderCount())
                .recoveryTeamStockKey(recoveryTeamStockKey)
                .build();
    }

}
