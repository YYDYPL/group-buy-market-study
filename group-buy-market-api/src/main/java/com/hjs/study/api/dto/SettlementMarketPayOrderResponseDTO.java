package com.hjs.study.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 拼团营销订单结算成功响应。
 *
 * <p>用于向支付调用方确认本次外部交易已经关联到具体用户、队伍和活动。是否因本次结算达到
 * 成团目标并触发后续通知，由领域服务在内部处理。</p>
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @create 2025-02-02 16:09
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SettlementMarketPayOrderResponseDTO {

    /** 完成结算的用户 ID。 */
    private String userId;
    /** 本次订单所属的拼团队伍 ID。 */
    private String teamId;
    /** 队伍所属的拼团活动 ID。 */
    private Long activityId;
    /** 本次完成结算的外部交易单号。 */
    private String outTradeNo;

}
