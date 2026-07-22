package com.hjs.study.domain.trade.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 交易结算命令实体。
 * <p>
 * 它表达的是“外部支付成功后，系统应该结算哪一笔内部拼团交易”。
 * 与 {@link TradePaySuccessEntity} 不同，这个对象更偏内部结算入口参数，
 * 除了支付单号外，还把队伍和活动维度一起带上，方便直接进入拼团结算流程。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 交易结算订单实体
 * @create 2025-01-26 14:54
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TradePaySettlementEntity {

    /** 业务来源。 */
    private String source;
    /** 具体渠道。 */
    private String channel;
    /** 完成支付的用户 ID。 */
    private String userId;
    /** 所属拼团队伍 ID。 */
    private String teamId;
    /** 所属活动 ID。 */
    private Long activityId;
    /** 第三方支付或外部业务系统传入的交易单号。 */
    private String outTradeNo;

}
