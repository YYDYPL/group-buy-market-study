package com.hjs.study.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商城和运营后台共享的商品活动配置响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductConfigResponseDTO {

    private String goodsId;
    private String goodsName;
    private BigDecimal originalPrice;
    private BigDecimal deductionPrice;
    private BigDecimal payPrice;
    private String priceExplanation;
    private String category;
    private String subtitle;
    private String mainImage;
    private List<String> galleryImages;
    private Integer salesCount;
    private BigDecimal favorableRate;
    private List<String> serviceTags;
    private Integer sortOrder;
    private Integer productStatus;
    private Integer version;

    private Long activityId;
    private String activityName;
    private String discountId;
    private Integer groupType;
    private Integer takeLimitCount;
    private Integer target;
    private Integer validTime;
    private Integer activityStatus;
    private String startTime;
    private String endTime;

    private String discountName;
    private String discountDesc;
    private Integer discountType;
    private String marketPlan;
    private String marketExpr;
    private String tagId;

    private String source;
    private String channel;
}
