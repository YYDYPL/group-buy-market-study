package com.hjs.study.domain.trade.service;

import com.hjs.study.domain.trade.model.entity.TradePaySettlementEntity;
import com.hjs.study.domain.trade.model.entity.TradePaySuccessEntity;

/**
 * 拼团交易结算服务接口。
 * <p>
 * 该接口承接外部支付成功信号，把支付结果正式转换为内部拼团结算动作。
 * 结算时不仅会更新单笔订单状态，还可能联动拼团队伍进度、成团判定和通知任务生成。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 拼团交易结算服务接口
 * @create 2025-01-26 14:34
 */
public interface ITradeSettlementOrderService {

    /**
     * 执行营销交易结算。
     * <p>
     * 输入是一笔已经支付成功的外部交易事件，
     * 输出是这笔交易在拼团系统内完成结算后的结果快照。
     *
     * @param tradePaySuccessEntity 交易支付订单实体对象
     * @return 交易结算订单实体
     */
    TradePaySettlementEntity settlementMarketPayOrder(TradePaySuccessEntity tradePaySuccessEntity) throws Exception;

}
