package com.hjs.study.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 商城订单聚合查询 PO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreOrder {

    private String userId;
    private String orderId;
    private String outTradeNo;
    private String goodsId;
    private String goodsName;
    private String mainImage;
    private Long activityId;
    private String teamId;
    private String source;
    private String channel;
    private BigDecimal originalPrice;
    private BigDecimal deductionPrice;
    private BigDecimal payPrice;
    private Integer orderStatus;
    private Date outTradeTime;
    private Date createTime;
    private Date updateTime;
    private Integer teamStatus;
    private Integer targetCount;
    private Integer completeCount;
    private Integer lockCount;
    private Date validEndTime;
    private Integer offset;
    private Integer pageSize;
    private Integer status;
}
