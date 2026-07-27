package com.hjs.study.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 商城拼团队伍详情响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreTeamResponseDTO {

    private String teamId;
    private Long activityId;
    private String source;
    private String channel;
    private BigDecimal originalPrice;
    private BigDecimal deductionPrice;
    private BigDecimal payPrice;
    private Integer targetCount;
    private Integer completeCount;
    private Integer lockCount;
    private Integer status;
    private String statusText;
    private Date validStartTime;
    private Date validEndTime;
    private Boolean canJoin;
    private List<Member> members;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Member {
        private String userId;
        private String orderId;
        private String outTradeNo;
        private Integer orderStatus;
        private String orderStatusText;
        private Date outTradeTime;
        private Date createTime;
    }
}
