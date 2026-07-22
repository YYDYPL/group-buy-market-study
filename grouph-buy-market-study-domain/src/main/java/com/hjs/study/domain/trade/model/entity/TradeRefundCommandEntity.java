package com.hjs.study.domain.trade.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 退款命令实体。
 * <p>
 * 它表示外部调用方发起了一次退款请求，领域层会据此去定位内部订单，
 * 再判断这笔单当前属于哪种退款场景。
 * 这是一个“输入命令”，所以只保留定位订单和路由业务所必需的数据。
 *
 * @author xiaofuge bugstack.cn @小傅哥
 * 2025/7/8 08:03
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TradeRefundCommandEntity {

    /** 发起退款的用户 ID。 */
    private String userId;

    /** 外部交易单号，用于反查内部预购订单和支付状态。 */
    private String outTradeNo;

    /** 业务来源。 */
    private String source;

    /** 具体渠道。 */
    private String channel;

}
