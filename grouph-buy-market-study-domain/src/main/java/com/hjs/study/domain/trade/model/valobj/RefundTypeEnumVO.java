package com.hjs.study.domain.trade.model.valobj;

import com.hjs.study.types.enums.GroupBuyOrderEnumVO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Arrays;

/**
 * 退款类型枚举。
 * <p>
 * 这个枚举不是简单做状态码翻译，而是把“退款场景识别逻辑”和“对应策略名”绑定在一起，
 * 属于典型的“枚举驱动策略分发”设计。
 * <p>
 * 领域层只需要把拼团队伍状态和交易订单状态传进来，
 * 就能定位出当前应走哪一种退款策略。
 *
 * @author xiaofuge bugstack.cn @小傅哥
 * 2025/7/11 18:59
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum RefundTypeEnumVO {

    /**
     * 未支付、未成团。
     * 通常说明只是占了拼团名额，还没有形成真实支付成功，
     * 所以主要做锁单回滚。
     */
    UNPAID_UNLOCK("unpaid_unlock", "unpaid2RefundStrategy", "未支付，未成团") {
        @Override
        public boolean matches(GroupBuyOrderEnumVO groupBuyOrderEnumVO, TradeOrderStatusEnumVO tradeOrderStatusEnumVO) {
            return GroupBuyOrderEnumVO.PROGRESS.equals(groupBuyOrderEnumVO) && TradeOrderStatusEnumVO.CREATE.equals(tradeOrderStatusEnumVO);
        }
    },
    
    /**
     * 已支付、未成团。
     * 这类场景说明用户已付款，但团队尚未达到成团条件，
     * 退款时既要处理订单关闭，也要回退团队支付进度。
     */
    PAID_UNFORMED("paid_unformed", "paid2RefundStrategy", "已支付，未成团") {
        @Override
        public boolean matches(GroupBuyOrderEnumVO groupBuyOrderEnumVO, TradeOrderStatusEnumVO tradeOrderStatusEnumVO) {
            return GroupBuyOrderEnumVO.PROGRESS.equals(groupBuyOrderEnumVO) && TradeOrderStatusEnumVO.COMPLETE.equals(tradeOrderStatusEnumVO);
        }
    },
    
    /**
     * 已支付、已成团。
     * 这是影响范围最大的一类退款，可能牵涉整团状态回滚与补偿通知。
     */
    PAID_FORMED("paid_formed", "paidTeam2RefundStrategy", "已支付，已成团") {
        @Override
        public boolean matches(GroupBuyOrderEnumVO groupBuyOrderEnumVO, TradeOrderStatusEnumVO tradeOrderStatusEnumVO) {
            // 完成、完成含退单，都归入“已支付且已成团”退款策略。
            return (GroupBuyOrderEnumVO.COMPLETE.equals(groupBuyOrderEnumVO) || GroupBuyOrderEnumVO.COMPLETE_FAIL.equals(groupBuyOrderEnumVO))
                    && TradeOrderStatusEnumVO.COMPLETE.equals(tradeOrderStatusEnumVO);
        }
    },
    ;

    private String code;
    private String strategy;
    private String info;

    /**
     * 判断当前拼团状态 + 交易状态是否命中该退款场景。
     * 每个枚举值自己维护匹配规则，避免在外部堆砌大量 if/else。
     */
    public abstract boolean matches(GroupBuyOrderEnumVO groupBuyOrderEnumVO, TradeOrderStatusEnumVO tradeOrderStatusEnumVO);

    /**
     * 根据拼团队伍状态与交易订单状态，定位对应退款策略。
     * 这一步相当于“退款路由器”。
     */
    public static RefundTypeEnumVO getRefundStrategy(GroupBuyOrderEnumVO groupBuyOrderEnumVO, TradeOrderStatusEnumVO tradeOrderStatusEnumVO) {
        return Arrays.stream(values())
                .filter(refundType -> refundType.matches(groupBuyOrderEnumVO, tradeOrderStatusEnumVO))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("不支持的退款状态组合: groupBuyOrderStatus=" + groupBuyOrderEnumVO + ", tradeOrderStatus=" + tradeOrderStatusEnumVO));
    }

    /**
     * 根据编码值反查枚举。
     * 常用于消息反序列化、补偿任务恢复等只拿到字符串编码的场景。
     */
    public static RefundTypeEnumVO getRefundTypeEnumVOByCode(String code) {
        switch (code) {
            case "unpaid_unlock":
                return UNPAID_UNLOCK;
            case "paid_unformed":
                return PAID_UNFORMED;
            case "paid_formed":
                return PAID_FORMED;
        }
        throw new RuntimeException("退单类型枚举值不存在: " + code);
    }

}
