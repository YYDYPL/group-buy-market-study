package com.hjs.study.domain.activity.model.entity;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 首页拼团试算请求实体。
 * <p>
 * 该实体承载的是“用户准备查看某个商品拼团活动时”的最小输入参数，
 * 包括用户、活动、商品和渠道来源信息。
 * 这些字段会作为试算流程树的输入，从根节点一路传递到营销计算节点。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 营销商品实体信息，通过这样一个信息获取商品优惠信息
 * @create 2024-12-14 13:46
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MarketProductEntity {

    /** 活动 ID；如果前端已明确指定活动，可直接使用该值。 */
    private Long activityId;
    /** 用户 ID；用于切量、标签判断、优惠试算等流程。 */
    private String userId;
    /** 商品 ID；用于查询商品基础信息和渠道商品活动映射。 */
    private String goodsId;
    /** 来源标识；通常表示业务来源或投放来源。 */
    private String source;
    /** 渠道标识；通常表示更细粒度的渠道维度。 */
    private String channel;

}
