package com.hjs.study.domain.trade.model.entity;

import com.hjs.study.domain.trade.model.valobj.NotifyConfigVO;
import com.hjs.study.types.enums.GroupBuyOrderEnumVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 结算规则过滤反馈实体。
 * <p>
 * 当结算规则校验完成后，系统会把当前队伍的关键快照打包成该对象返回。
 * 后续仓储在真正落库结算时，可以直接使用这里的进度与回调信息，
 * 避免再次查询并减少结算过程中的上下文丢失。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 拼团交易结算规则反馈
 * @create 2025-01-29 09:53
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TradeSettlementRuleFilterBackEntity {

    /** 当前待结算的拼团队伍 ID。 */
    private String teamId;
    /** 所属活动 ID。 */
    private Long activityId;
    /** 成团目标数量。 */
    private Integer targetCount;
    /** 当前已支付完成数量。 */
    private Integer completeCount;
    /** 当前锁单数量，结算时通常会同步参与状态判断。 */
    private Integer lockCount;
    /** 队伍当前状态，用于判断结算后是否需要切换为成团完成等状态。 */
    private GroupBuyOrderEnumVO status;
    /** 队伍创建/生效开始时间。 */
    private Date validStartTime;
    /** 队伍截止失效时间。 */
    private Date validEndTime;
    /** 结算完成后可复用的回调配置。 */
    private NotifyConfigVO notifyConfigVO;

}
