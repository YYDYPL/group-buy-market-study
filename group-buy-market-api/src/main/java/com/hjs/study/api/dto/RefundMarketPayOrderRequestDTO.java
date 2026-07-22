package com.hjs.study.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 拼团营销退单请求。
 *
 * <p>用户 ID 与外部交易单号用于定位待处理订单，来源和渠道用于保持原交易业务上下文。调用方
 * 不需要指定退单策略，领域服务会根据订单当前状态选择未支付、已支付或成团后退单流程。</p>
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @create 2025-01-01 00:00
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RefundMarketPayOrderRequestDTO {

    /**
     * 申请退单的用户 ID，必须与原锁单用户一致。
     */
    private String userId;

    /**
     * 原锁单请求使用的外部交易单号，与用户 ID 共同定位订单。
     */
    private String outTradeNo;

    /**
     * 原交易的业务来源标识。
     */
    private String source;

    /**
     * 原交易的业务渠道标识。
     */
    private String channel;

}
