package com.hjs.study.api;

import com.hjs.study.api.dto.GoodsMarketRequestDTO;
import com.hjs.study.api.dto.GoodsMarketResponseDTO;
import com.hjs.study.api.response.Response;

/**
 * 拼团营销首页查询服务契约。
 *
 * <p>面向商品详情页一次性提供营销试算价格、当前可参与队伍和活动统计。该接口定义对外协议，
 * 活动匹配、人群标签和优惠计算由领域层完成。</p>
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @create 2025-02-02 16:02
 */
public interface IMarketIndexService {

    /**
     * 查询指定用户、来源、渠道和商品上下文中的拼团营销配置。
     *
     * @param goodsMarketRequestDTO 营销查询条件，不应为 {@code null}，且各标识字段均需有效
     * @return 成功时携带商品价格、可参与队伍和活动统计；失败时由统一响应返回错误码
     */
    Response<GoodsMarketResponseDTO> queryGroupBuyMarketConfig(GoodsMarketRequestDTO goodsMarketRequestDTO);

}
