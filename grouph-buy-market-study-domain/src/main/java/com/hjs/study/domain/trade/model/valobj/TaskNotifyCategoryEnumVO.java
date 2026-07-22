package com.hjs.study.domain.trade.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 通知任务分类枚举。
 * <p>
 * 交易域会把不同业务阶段产生的外部通知统一沉淀为任务，
 * 但任务的来源和含义并不相同。这个枚举用于区分：
 * 是正常交易结算通知，还是不同退款场景下的补偿通知。
 *
 * @author xiaofuge bugstack.cn @小傅哥
 * 2025/7/18 21:35
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum TaskNotifyCategoryEnumVO {

    /** 正常支付结算成功后产生的通知任务。 */
    TRADE_SETTLEMENT("trade_settlement","交易结算"),
    /** 未支付且未成团退款场景产生的通知任务。 */
    TRADE_UNPAID2REFUND("trade_unpaid2refund","交易退单-未支付&未成团"),
    /** 已支付但未成团退款场景产生的通知任务。 */
    TRADE_PAID2REFUND("trade_paid2refund","交易退单-已支付&未成团"),
    /** 已支付且已成团退款场景产生的通知任务。 */
    TRADE_PAID_TEAM2REFUND("trade_paid_team2refund","交易退单-已支付&已成团"),

    ;

    private String code;
    private String info;

    /**
     * 根据字符串编码反查任务分类枚举。
     * 主要用于通知任务表、MQ 消息或补偿任务中仅持有 code 的场景。
     */
    public static TaskNotifyCategoryEnumVO getByCode(String code) {
        for (TaskNotifyCategoryEnumVO enumVO : values()) {
            if (enumVO.getCode().equals(code)) {
                return enumVO;
            }
        }
        throw new RuntimeException("任务通知类型枚举值不存在: " + code);
    }

}
