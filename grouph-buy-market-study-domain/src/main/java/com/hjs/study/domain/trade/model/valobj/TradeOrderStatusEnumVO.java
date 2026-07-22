package com.hjs.study.domain.trade.model.valobj;

import lombok.*;

/**
 * 交易订单状态枚举。
 * <p>
 * 它描述的是单笔预购订单自身的生命周期状态，
 * 注意要和“拼团队伍状态”区分开：
 * 队伍关注整团是否完成，订单关注单笔交易是否创建、支付完成或关闭。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 交易订单状态枚举
 * @create 2025-01-11 10:21
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum TradeOrderStatusEnumVO {

    /** 订单已创建，但通常还未完成支付。 */
    CREATE(0, "初始创建"),
    /** 订单已支付完成并进入成交状态。 */
    COMPLETE(1, "消费完成"),
    /** 订单已关闭，常见于退款或取消场景。 */
    CLOSE(2, "用户退单"),
    ;

    private Integer code;
    private String info;

    /**
     * 根据状态码转换为枚举。
     * 数据库中通常存整数值，进入领域层后转换为枚举可提升可读性和类型安全。
     */
    public static TradeOrderStatusEnumVO valueOf(Integer code) {
        switch (code) {
            case 0:
                return CREATE;
            case 1:
                return COMPLETE;
            case 2:
                return CLOSE;
        }
        return CREATE;
    }

}
