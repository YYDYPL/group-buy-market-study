package com.hjs.study.domain.trade.model.entity;

import lombok.*;

/**
 * 退款行为结果实体。
 * <p>
 * 退款接口被调用后，领域层除了执行实际退款逻辑，还需要把“这次调用的处理结果”反馈给上层。
 * 例如是首次成功退款、重复退款请求，还是处理失败。
 * 这个实体就承担“退款动作执行回执”的职责。
 *
 * @author xiaofuge bugstack.cn @小傅哥
 * 2025/7/12 07:50
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TradeRefundBehaviorEntity {

    /** 发起退款所属的用户 ID。 */
    private String userId;

    /** 内部预购订单号，用于标识具体是哪一笔子单在退款。 */
    private String orderId;

    /** 所属拼团队伍 ID。 */
    private String teamId;

    /** 退款处理结果枚举，用于给应用层返回明确的行为语义。 */
    private TradeRefundBehaviorEnum tradeRefundBehaviorEnum;

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    /**
     * 退款行为枚举。
     * 不同于退款类型枚举，这里描述的是“执行结果”，不是“业务场景分类”。
     */
    public enum TradeRefundBehaviorEnum {

        /** 本次退款请求已成功处理。 */
        SUCCESS("success", "成功"),
        /** 该退款请求已被处理过，属于重复调用。 */
        REPEAT("repeat", "重复"),
        /** 本次退款处理失败。 */
        FAIL("fail", "失败"),

        ;

        private String code;
        private String info;
    }

}
