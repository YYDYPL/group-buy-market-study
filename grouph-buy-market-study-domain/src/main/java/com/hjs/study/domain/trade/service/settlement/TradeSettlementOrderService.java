package com.hjs.study.domain.trade.service.settlement;

import com.alibaba.fastjson.JSON;
import com.hjs.study.domain.trade.adapter.repository.ITradeRepository;
import com.hjs.study.domain.trade.model.aggregate.GroupBuyTeamSettlementAggregate;
import com.hjs.study.domain.trade.model.entity.GroupBuyTeamEntity;
import com.hjs.study.domain.trade.model.entity.NotifyTaskEntity;
import com.hjs.study.domain.trade.model.entity.TradePaySettlementEntity;
import com.hjs.study.domain.trade.model.entity.TradePaySuccessEntity;
import com.hjs.study.domain.trade.model.entity.TradeSettlementRuleCommandEntity;
import com.hjs.study.domain.trade.model.entity.TradeSettlementRuleFilterBackEntity;
import com.hjs.study.domain.trade.model.entity.UserEntity;
import com.hjs.study.domain.trade.service.ITradeSettlementOrderService;
import com.hjs.study.domain.trade.service.ITradeTaskService;
import com.hjs.study.domain.trade.service.settlement.factory.TradeSettlementRuleFilterFactory;
import com.hjs.study.types.design.framework.link.model2.chain.BusinessLinkedList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

/** 支付成功后的拼团交易结算服务。 */
@Slf4j
@Service
public class TradeSettlementOrderService implements ITradeSettlementOrderService {

    @Resource
    private ITradeRepository repository;

    @Resource
    private ITradeTaskService tradeTaskService;

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @Resource
    private BusinessLinkedList<TradeSettlementRuleCommandEntity,
            TradeSettlementRuleFilterFactory.DynamicContext,
            TradeSettlementRuleFilterBackEntity> tradeSettlementRuleFilter;

    @Override
    public TradePaySettlementEntity settlementMarketPayOrder(TradePaySuccessEntity tradePaySuccessEntity) throws Exception {
        log.info("拼团交易-结算支付订单:{} outTradeNo:{}",
                tradePaySuccessEntity.getUserId(), tradePaySuccessEntity.getOutTradeNo());

        TradeSettlementRuleFilterBackEntity filterBackEntity = tradeSettlementRuleFilter.apply(
                TradeSettlementRuleCommandEntity.builder()
                        .source(tradePaySuccessEntity.getSource())
                        .channel(tradePaySuccessEntity.getChannel())
                        .userId(tradePaySuccessEntity.getUserId())
                        .outTradeNo(tradePaySuccessEntity.getOutTradeNo())
                        .outTradeTime(tradePaySuccessEntity.getOutTradeTime())
                        .build(),
                new TradeSettlementRuleFilterFactory.DynamicContext());

        GroupBuyTeamEntity groupBuyTeamEntity = GroupBuyTeamEntity.builder()
                .teamId(filterBackEntity.getTeamId())
                .activityId(filterBackEntity.getActivityId())
                .targetCount(filterBackEntity.getTargetCount())
                .completeCount(filterBackEntity.getCompleteCount())
                .lockCount(filterBackEntity.getLockCount())
                .status(filterBackEntity.getStatus())
                .validStartTime(filterBackEntity.getValidStartTime())
                .validEndTime(filterBackEntity.getValidEndTime())
                .notifyConfigVO(filterBackEntity.getNotifyConfigVO())
                .build();

        NotifyTaskEntity notifyTaskEntity = repository.settlementMarketPayOrder(GroupBuyTeamSettlementAggregate.builder()
                .userEntity(UserEntity.builder().userId(tradePaySuccessEntity.getUserId()).build())
                .groupBuyTeamEntity(groupBuyTeamEntity)
                .tradePaySuccessEntity(tradePaySuccessEntity)
                .build());

        if (null != notifyTaskEntity) {
            threadPoolExecutor.execute(() -> {
                Map<String, Integer> notifyResultMap = null;
                try {
                    notifyResultMap = tradeTaskService.execNotifyJob(notifyTaskEntity);
                    log.info("回调通知拼团结算 result:{}", JSON.toJSONString(notifyResultMap));
                } catch (Exception e) {
                    log.error("回调通知拼团结算失败 result:{}", JSON.toJSONString(notifyResultMap), e);
                }
            });
        }

        return TradePaySettlementEntity.builder()
                .source(tradePaySuccessEntity.getSource())
                .channel(tradePaySuccessEntity.getChannel())
                .userId(tradePaySuccessEntity.getUserId())
                .teamId(groupBuyTeamEntity.getTeamId())
                .activityId(groupBuyTeamEntity.getActivityId())
                .outTradeNo(tradePaySuccessEntity.getOutTradeNo())
                .build();
    }

}
