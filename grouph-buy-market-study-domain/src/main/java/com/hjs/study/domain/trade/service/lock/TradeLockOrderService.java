package com.hjs.study.domain.trade.service.lock;

import com.hjs.study.domain.trade.adapter.repository.ITradeRepository;
import com.hjs.study.domain.trade.model.aggregate.GroupBuyOrderAggregate;
import com.hjs.study.domain.trade.model.entity.*;
import com.hjs.study.domain.trade.model.valobj.GroupBuyProgressVO;
import com.hjs.study.domain.trade.service.ITradeLockOrderService;
import com.hjs.study.domain.trade.service.lock.factory.TradeLockRuleFilterFactory;
import com.hjs.study.types.design.framework.link.model2.chain.BusinessLinkedList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 拼团锁单领域服务实现。
 * <p>
 * 该服务负责把“用户准备参与拼团”这件事真正落成一笔预购订单。
 * 整体流程可以概括为：
 * 1. 先执行锁单责任链，校验活动、参与次数、团队库存。
 * 2. 把规则链产出的辅助数据与用户/活动/优惠信息组装成聚合。
 * 3. 调用仓储落库生成拼团主单/子单，并在失败时做库存回补。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 交易订单服务
 * @create 2025-01-11 08:07
 */
@Slf4j
@Service
public class TradeLockOrderService implements ITradeLockOrderService {

    /** 交易仓储，负责真正落订单、查进度、回补库存等持久化动作。 */
    @Resource
    private ITradeRepository repository;
    /** 锁单责任链，按顺序执行活动可用性、参与上限、团队库存占用等规则。 */
    @Resource
    private BusinessLinkedList<TradeLockRuleCommandEntity, TradeLockRuleFilterFactory.DynamicContext, TradeLockRuleFilterBackEntity> tradeRuleFilter;

    @Override
    public MarketPayOrderEntity queryNoPayMarketPayOrderByOutTradeNo(String userId, String outTradeNo) {
        // 通过外部单号查询未支付订单，是外部幂等控制和重复点击防重的重要支撑点。
        log.info("拼团交易-查询未支付营销订单:{} outTradeNo:{}", userId, outTradeNo);
        return repository.queryMarketPayOrderEntityByOutTradeNo(userId, outTradeNo);
    }

    @Override
    public GroupBuyProgressVO queryGroupBuyProgress(String teamId) {
        // 当前实现只做简单转发，把团队进度查询职责统一收口到交易领域服务层。
        log.info("拼团交易-查询拼单进度:{}", teamId);
        return repository.queryGroupBuyProgress(teamId);
    }

    @Override
    public MarketPayOrderEntity lockMarketPayOrder(UserEntity userEntity, PayActivityEntity payActivityEntity, PayDiscountEntity payDiscountEntity) throws Exception {
        log.info("拼团交易-锁定营销优惠支付订单:{} activityId:{} goodsId:{}", userEntity.getUserId(), payActivityEntity.getActivityId(), payDiscountEntity.getGoodsId());

        // 先走规则链，把“能不能锁单”以及“后续锁单需要的辅助上下文”一次性准备好。
        TradeLockRuleFilterBackEntity tradeLockRuleFilterBackEntity = tradeRuleFilter.apply(TradeLockRuleCommandEntity.builder()
                        .activityId(payActivityEntity.getActivityId())
                        .userId(userEntity.getUserId())
                        .teamId(payActivityEntity.getTeamId())
                        .build(),
                new TradeLockRuleFilterFactory.DynamicContext());

        // 已参与次数不是为了展示，而是为了构建业务唯一索引，防止用户超限参与。
        Integer userTakeOrderCount = tradeLockRuleFilterBackEntity.getUserTakeOrderCount();

        // 使用聚合把用户、活动、优惠、参与次数组合成一次完整的锁单业务动作。
        GroupBuyOrderAggregate groupBuyOrderAggregate = GroupBuyOrderAggregate.builder()
                .userEntity(userEntity)
                .payActivityEntity(payActivityEntity)
                .payDiscountEntity(payDiscountEntity)
                .userTakeOrderCount(userTakeOrderCount)
                .build();

        try {
            // 仓储负责真正生成预购订单。此时只是锁单成功，还没完成支付。
            // 后续会继续分流为“支付成功结算”或“超时未支付回退”两条链路。
            return repository.lockMarketPayOrder(groupBuyOrderAggregate);
        } catch (Exception e) {
            // 如果锁单落库失败，要把前面预占的团队库存补回去，避免团队名额被脏占用。
            repository.recoveryTeamStock(tradeLockRuleFilterBackEntity.getRecoveryTeamStockKey(), payActivityEntity.getValidTime());
            throw e;
        }

    }

}
