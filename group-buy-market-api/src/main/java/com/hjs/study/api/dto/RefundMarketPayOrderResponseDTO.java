package com.hjs.study.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 拼团营销退单成功响应。
 *
 * <p>除订单和队伍标识外，还返回领域层实际执行的退单行为码。外层统一响应码表示接口是否成功
 * 处理，当前对象中的 {@link #code} 与 {@link #info} 则描述具体退单策略结果。</p>
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @create 2025-01-01 00:00
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RefundMarketPayOrderResponseDTO {

    /**
     * 已处理订单所属的用户 ID。
     */
    private String userId;

    /**
     * 系统内部营销订单 ID。
     */
    private String orderId;

    /**
     * 退单影响的拼团队伍 ID。
     */
    private String teamId;

    /**
     * 领域退单行为状态码，用于区分不同订单状态下采取的处理方式。
     */
    private String code;

    /**
     * 与退单行为状态码对应的可读说明。
     */
    private String info;

}
