package com.hjs.study.domain.trade.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 结算规则命令实体。
 * <p>
 * 它表示“系统收到一笔支付成功通知，准备进入拼团结算规则判断”时的输入参数。
 * 规则层拿到它后，会去判断对应队伍是否仍有效、当前进度如何、结算后是否应该成团。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 拼团交易结算规则命令
 * @create 2025-01-29 09:55
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TradeSettlementRuleCommandEntity {

    /** 业务来源。 */
    private String source;
    /** 具体渠道。 */
    private String channel;
    /** 支付成功所属用户 ID。 */
    private String userId;
    /** 外部交易单号，用于关联内部待结算订单。 */
    private String outTradeNo;
    /** 外部支付成功时间，用于记录结算时刻和顺序。 */
    private Date outTradeTime;

}
