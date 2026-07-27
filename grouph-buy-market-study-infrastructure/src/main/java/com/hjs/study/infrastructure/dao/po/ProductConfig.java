package com.hjs.study.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 后台商品配置聚合查询 PO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductConfig {

    private String goodsId;
    private String goodsName;
    private BigDecimal originalPrice;
    private String category;
    private String subtitle;
    private String mainImage;
    private String galleryImages;
    private Integer salesCount;
    private BigDecimal favorableRate;
    private String serviceTags;
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
    private Date startTime;
    private Date endTime;

    private String discountName;
    private String discountDesc;
    private Integer discountType;
    private String marketPlan;
    private String marketExpr;
    private String tagId;
    private String source;
    private String channel;
    /** 商品资料的草稿或发布快照，避免编辑草稿时污染线上商品。 */
    private String draftData;

    private String keyword;
    private Integer status;
    private String sort;
    private Integer offset;
    private Integer pageSize;
}
