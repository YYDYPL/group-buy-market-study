package com.hjs.study.domain.trade.adapter.repository;

import com.hjs.study.domain.trade.model.aggregate.GroupBuyOrderAggregate;
import com.hjs.study.domain.trade.model.entity.MarketPayOrderEntity;
import com.hjs.study.domain.trade.model.valobj.GroupBuyProgressVO;

public interface ITradeRepository {

    MarketPayOrderEntity queryMarketPayOrderEntityByOutTradeNo(String userId, String outTradeNo);

    MarketPayOrderEntity lockMarketPayOrder(GroupBuyOrderAggregate groupBuyOrderAggregate);

    GroupBuyProgressVO queryGroupBuyProgress(String teamId);

}
