package com.hjs.study.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 拼团营销锁单成功响应。
 *
 * <p>返回调用方继续创建支付单所需的内部订单、服务端价格快照和队伍信息。重复提交同一外部
 * 交易单号时，接口可能返回既有待支付订单的相同数据。</p>
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @create 2025-01-11 13:56
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LockMarketPayOrderResponseDTO {

    /** 系统生成的营销预购订单 ID，用于内部订单关联。 */
    private String orderId;
    /** 锁单时服务端确认的商品原价。 */
    private BigDecimal originalPrice;
    /** 锁单时命中活动计算出的优惠抵扣金额。 */
    private BigDecimal deductionPrice;
    /** 调用方后续应支付的实际成交金额。 */
    private BigDecimal payPrice;
    /** 交易订单状态码，具体含义由交易状态枚举定义。 */
    private Integer tradeOrderStatus;
    /** 锁单实际归属的队伍 ID；开新团时由服务端创建并返回。 */
    private String teamId;

}
