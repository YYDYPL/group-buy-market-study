package com.hjs.study.domain.admin.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 商品、优惠、拼团活动和渠道路由组成的后台配置聚合。
 *
 * <p>后台始终以一个聚合完成草稿保存和发布，避免商品已更新但活动或路由未更新的
 * 半完成状态。轮播图和服务标签使用 JSON 字符串保存，由 HTTP 层负责与数组互转。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductConfigEntity {

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
    /** 商品状态：0 草稿、1 上架、2 下架。 */
    private Integer productStatus;
    /** 商品乐观锁版本。 */
    private Integer version;

    private Long activityId;
    private String activityName;
    private String discountId;
    private Integer groupType;
    private Integer takeLimitCount;
    private Integer target;
    private Integer validTime;
    /** 活动状态：0 草稿、1 生效、2 下架、3 废弃。 */
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
}
