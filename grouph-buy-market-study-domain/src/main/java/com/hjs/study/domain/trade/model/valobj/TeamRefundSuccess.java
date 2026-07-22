package com.hjs.study.domain.trade.model.valobj;

import lombok.*;

/**
 * 拼团退款成功消息体。
 * <p>
 * 当退款流程完成后，系统往往需要把结果推送给下游系统或消息总线。
 * 这个值对象就是那份“可序列化的退款成功消息载荷”，
 * 通常会被放进通知任务或 MQ 消息中。
 *
 * @author xiaofuge bugstack.cn @小傅哥
 * 2025/7/29 09:15
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TeamRefundSuccess {

    /** 退款类型编码，例如未支付退款、已成团退款等。 */
    private String type;

    /** 退款所属用户 ID。 */
    private String userId;

    /** 退款所属拼团队伍 ID。 */
    private String teamId;

    /** 退款所属活动 ID。 */
    private Long activityId;

    /** 内部预购订单号。 */
    private String orderId;

    /** 外部交易单号。 */
    private String outTradeNo;

}
