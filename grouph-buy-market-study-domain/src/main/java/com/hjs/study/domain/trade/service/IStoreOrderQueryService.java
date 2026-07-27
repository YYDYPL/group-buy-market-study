package com.hjs.study.domain.trade.service;

import com.hjs.study.domain.trade.model.entity.StoreOrderEntity;
import com.hjs.study.domain.trade.model.entity.StoreTeamEntity;

import java.util.List;

/**
 * 商城订单查询领域服务。
 */
public interface IStoreOrderQueryService {

    List<StoreOrderEntity> queryUserOrders(
            String userId, String goodsId, Integer status, Integer page, Integer pageSize);

    Integer countUserOrders(String userId, String goodsId, Integer status);

    StoreTeamEntity queryTeam(String teamId);
}
