package com.hjs.study.api;

import com.hjs.study.api.dto.StoreOrderPageResponseDTO;
import com.hjs.study.api.dto.StoreTeamResponseDTO;
import com.hjs.study.api.response.Response;

/**
 * 商城订单与拼团队伍查询 API。
 */
public interface IStoreOrderService {

    Response<StoreOrderPageResponseDTO> queryUserOrders(
            String userId, String goodsId, Integer status, Integer page, Integer pageSize);

    Response<StoreTeamResponseDTO> queryTeam(String teamId);
}
