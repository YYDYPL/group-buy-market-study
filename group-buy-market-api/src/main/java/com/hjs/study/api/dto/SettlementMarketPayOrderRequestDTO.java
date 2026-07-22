package com.hjs.study.api.dto;

import lombok.Data;

import java.util.Date;

/**
 * 拼团营销订单支付结算请求。
 *
 * <p>该对象描述外部支付系统已经确认的支付成功事实，不代表发起收款。结算服务会根据这些字段
 * 查找待支付锁单、校验支付时间，并推进用户订单和队伍成团进度。</p>
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @create 2025-02-02 16:08
 */
@Data
public class SettlementMarketPayOrderRequestDTO {

    /** 原交易的业务来源标识。 */
    private String source;
    /** 原交易的业务渠道标识。 */
    private String channel;
    /** 完成支付的用户 ID，必须与锁单记录一致。 */
    private String userId;
    /** 支付系统和营销订单共同使用的外部交易单号。 */
    private String outTradeNo;
    /** 外部支付实际完成时间，用于校验是否落在活动和队伍有效期内。 */
    private Date outTradeTime;

}
