package com.hjs.study.domain.trade.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 退款目标订单实体。
 * <p>
 * 退款命令进入系统后，领域层会先把外部输入转换、查询并补全成这个对象，
 * 表示“已经定位到具体要退款的内部订单”。后续构造退款聚合、执行退款策略时，
 * 都是围绕这个对象继续展开。
 *
 * @author xiaofuge bugstack.cn @小傅哥
 * 2025/7/11 19:45
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TradeRefundOrderEntity {

    /** 订单所属用户 ID。 */
    private String userId;

    /** 订单所属拼团队伍 ID。 */
    private String teamId;

    /** 订单所属活动 ID。 */
    private Long activityId;

    /** 内部预购订单 ID，是执行退单更新时的直接索引。 */
    private String orderId;

    /** 外部交易单号，用于和外部支付流水保持关联。 */
    private String outTradeNo;

}
