package com.hjs.study.domain.trade.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 锁单规则过滤反馈实体。
 * <p>
 * 规则校验通过后，领域服务除了返回“能不能下单”之外，还会顺手把后续链路要用的辅助数据带出来，
 * 这样仓储就不需要重复查一遍数据库或缓存。
 * 这个对象就是规则过滤阶段输出给后续锁单动作的补充上下文。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 拼团交易，过滤反馈实体
 * @create 2025-01-25 14:16
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TradeLockRuleFilterBackEntity {

    /** 用户当前已参与该活动的订单量，用于限次校验和构建幂等业务号。 */
    private Integer userTakeOrderCount;

    /**
     * 恢复团队库存的缓存 Key。
     * 当后续发生超时关闭、退款回滚时，可以通过这个 Key 把预占的团队库存补回去。
     */
    private String recoveryTeamStockKey;

}
