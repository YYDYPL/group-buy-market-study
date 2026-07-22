package com.hjs.study.domain.trade.model.entity;

import com.hjs.study.domain.trade.model.valobj.NotifyConfigVO;
import com.hjs.study.types.enums.GroupBuyOrderEnumVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 拼团队伍实体。
 * <p>
 * 这个实体对应的是一支具体的拼团队伍，可以理解为“主团单”的领域表达。
 * 它关注的是整支队伍的整体进度，而不是某个成员的个人订单。
 * 结算、退款、超时扫描、通知补偿，都会围绕这个实体上的进度字段展开。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 拼团组队实体对象
 * @create 2025-01-26 16:19
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GroupBuyTeamEntity {

    /** 队伍编号，同一支拼团队伍下的所有成员订单都会挂在这个 ID 下面。 */
    private String teamId;
    /** 所属活动 ID，用于说明该队伍来自哪场拼团活动。 */
    private Long activityId;
    /** 成团目标数量，即该队伍需要累计达到的人数/单数。 */
    private Integer targetCount;
    /** 已支付完成数量，通常表示已经真正完成支付并计入成团进度的人数。 */
    private Integer completeCount;
    /**
     * 锁单数量。
     * 表示已经占位但未必完成支付的订单数量，
     * 这个值在下单锁单、超时取消、退款回滚时都会发生变化。
     */
    private Integer lockCount;
    /** 队伍当前状态，例如拼团中、已完成、已失败。 */
    private GroupBuyOrderEnumVO status;
    /** 队伍开始时间，一般取首个成员发起拼团或加入拼团的时间点。 */
    private Date validStartTime;
    /** 队伍失效时间，超过这个时间未成团则通常进入失败或退款补偿链路。 */
    private Date validEndTime;
    /**
     * 回调配置。
     * 成团、退款等关键状态发生变化后，系统需要依据这里的配置决定是发 MQ 还是调 HTTP。
     */
    private NotifyConfigVO notifyConfigVO;

}
