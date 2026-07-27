package com.hjs.study.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 运营后台商品活动组合配置请求。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductConfigRequestDTO {

    private String goodsId;
    private String goodsName;
    private BigDecimal originalPrice;
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
