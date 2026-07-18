package com.hjs.study.domain.activity.model.entity;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MarketProductEntity {
    /** 活动ID */
    private Long activityId;

    private String userId;

    private String goodsId;

    private String source;

    private String channel;
}
