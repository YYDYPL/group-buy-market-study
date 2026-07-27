package com.hjs.study.domain.trade.service;

import com.hjs.study.domain.trade.adapter.repository.ITradeRepository;
import com.hjs.study.domain.trade.model.entity.StoreOrderEntity;
import com.hjs.study.domain.trade.model.entity.StoreTeamEntity;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 商城订单查询领域服务实现。
 */
@Service
public class StoreOrderQueryService implements IStoreOrderQueryService {

    private final ITradeRepository tradeRepository;

    public StoreOrderQueryService(ITradeRepository tradeRepository) {
        this.tradeRepository = tradeRepository;
    }

    @Override
    public List<StoreOrderEntity> queryUserOrders(
            String userId, String goodsId, Integer status, Integer page, Integer pageSize) {
        int safePage = Math.max(page == null ? 1 : page, 1);
        int safePageSize = Math.max(1, Math.min(pageSize == null ? 20 : pageSize, 50));
        return tradeRepository.queryStoreOrders(
                userId, goodsId, status, (safePage - 1) * safePageSize, safePageSize);
    }

    @Override
    public Integer countUserOrders(String userId, String goodsId, Integer status) {
        return tradeRepository.countStoreOrders(userId, goodsId, status);
    }

    @Override
    public StoreTeamEntity queryTeam(String teamId) {
        return tradeRepository.queryStoreTeam(teamId);
    }
}
