package com.hjs.study.domain.trade.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 支付成功事件实体。
 * <p>
 * 它描述的是外部支付系统已经确认成功的一笔交易事件，
 * 是“支付结果输入”在领域层中的表达。领域服务会根据它去查找内部预购订单，
 * 再推动拼团进度进入结算。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 交易支付订单实体对象
 * @create 2025-01-26 14:52
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TradePaySuccessEntity {

    /** 业务来源。 */
    private String source;
    /** 具体接入渠道。 */
    private String channel;
    /** 完成支付的用户 ID。 */
    private String userId;
    /** 外部支付单号，是关联内部预购订单的重要索引。 */
    private String outTradeNo;
    /** 支付平台确认成功的时间点，通常作为结算时间依据。 */
    private Date outTradeTime;

}
