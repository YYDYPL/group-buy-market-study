package com.hjs.study.domain.trade.service.refund;


import com.hjs.study.domain.activity.model.entity.UserGroupBuyOrderDetailEntity;
import com.hjs.study.domain.trade.adapter.repository.ITradeRepository;
import com.hjs.study.domain.trade.model.entity.*;
import com.hjs.study.domain.trade.model.valobj.RefundTypeEnumVO;
import com.hjs.study.domain.trade.model.valobj.TeamRefundSuccess;
import com.hjs.study.domain.trade.service.ITradeRefundOrderService;
import com.hjs.study.domain.trade.service.refund.business.IRefundOrderStrategy;
import com.hjs.study.domain.trade.service.refund.factory.TradeRefundRuleFilterFactory;

import com.hjs.study.types.design.framework.link.model2.chain.BusinessLinkedList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * 退款逆向流程领域服务。
 * <p>
 * 它是退款入口的总编排者，本身不直接写死每一种退款处理细节，
 * 而是通过“退款责任链 + 退款策略映射”的组合来完成：
 * 1. 责任链负责加载数据、做幂等检查、识别退款场景。
 * 2. 策略对象负责执行该场景下真正的库存/状态/通知回滚。
 *
 * @author xiaofuge bugstack.cn @小傅哥
 * 2025/7/8 07:27
 */
@Slf4j
@Service
public class TradeRefundOrderService implements ITradeRefundOrderService {

    /** 退款责任链，串起数据加载、重复退款检查、策略分发等节点。 */
    @Resource
    private BusinessLinkedList<TradeRefundCommandEntity, TradeRefundRuleFilterFactory.DynamicContext, TradeRefundBehaviorEntity> tradeRefundRuleFilter;

    /** 交易仓储，负责查询超时未支付订单等逆向流程数据。 */
    private final ITradeRepository repository;

    /** 退款策略映射表，key 一般是 Spring Bean 名称，对应不同退款场景。 */
    private final Map<String, IRefundOrderStrategy> refundOrderStrategyMap;

    public TradeRefundOrderService(ITradeRepository repository, Map<String, IRefundOrderStrategy> refundOrderStrategyMap) {
        this.repository = repository;
        this.refundOrderStrategyMap = refundOrderStrategyMap;
    }

    @Override
    public TradeRefundBehaviorEntity refundOrder(TradeRefundCommandEntity tradeRefundCommandEntity) throws Exception {
        log.info("逆向流程，退单操作 userId:{} outTradeNo:{}", tradeRefundCommandEntity.getUserId(), tradeRefundCommandEntity.getOutTradeNo());
        // 责任链内部会依次完成数据准备、幂等判断、退款策略执行，并最终返回行为结果。
        return tradeRefundRuleFilter.apply(tradeRefundCommandEntity, new TradeRefundRuleFilterFactory.DynamicContext());
    }

    @Override
    public void restoreTeamLockStock(TeamRefundSuccess teamRefundSuccess) throws Exception {
        log.info("逆向流程，恢复锁单量 userId:{} activityId:{} teamId:{}", teamRefundSuccess.getUserId(), teamRefundSuccess.getActivityId(), teamRefundSuccess.getTeamId());
        String type = teamRefundSuccess.getType();

        // 退款成功消息只带字符串类型，这里先反查到业务枚举，再定位具体策略。
        RefundTypeEnumVO refundTypeEnumVO = RefundTypeEnumVO.getRefundTypeEnumVOByCode(type);
        IRefundOrderStrategy refundOrderStrategy = refundOrderStrategyMap.get(refundTypeEnumVO.getStrategy());

        // 并不是所有退款都需要恢复锁单库存，是否恢复由具体策略决定。
        refundOrderStrategy.reverseStock(teamRefundSuccess);
    }

    @Override
    public List<UserGroupBuyOrderDetailEntity> queryTimeoutUnpaidOrderList() {
        log.info("扫描数据，超时组队未支付订单");
        // 供定时任务扫描使用，把需要触发超时回退的订单统一捞出来。
        return repository.queryTimeoutUnpaidOrderList();
    }

}
