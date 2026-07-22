package com.hjs.study.domain.activity.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 商品值对象。
 * <p>
 * 该对象只保留活动试算真正需要的商品字段，
 * 因此比完整的商品领域模型更轻量。
 * 试算链只关心商品标识、展示名称和原价，不需要承担库存、类目等更多维度信息。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 商品信息
 * @create 2024-12-21 10:45
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SkuVO {

    /** 商品 ID。 */
    private String goodsId;
    /** 商品名称。 */
    private String goodsName;
    /** 商品原价，用作各类优惠计算的基准。 */
    private BigDecimal originalPrice;

}
