package com.hjs.study.domain.activity.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 渠道商品活动映射值对象。
 * <p>
 * 当调用方只知道“来源 + 渠道 + 商品”而不知道活动 ID 时，
 * 可以先通过这个值对象把三元组路由到具体活动。
 * 它是首页试算、落单前活动匹配的关键中间结果。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 渠道商品活动配置值对象
 * @create 2025-01-01 09:38
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SCSkuActivityVO {

    /** 来源标识。 */
    private String source;
    /** 渠道标识。 */
    private String chanel;
    /** 命中的活动 ID。 */
    private Long activityId;
    /** 商品 ID。 */
    private String goodsId;

}
