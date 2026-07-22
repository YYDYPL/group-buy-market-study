package com.hjs.study.domain.trade.model.entity;

import com.hjs.study.types.enums.ActivityStatusEnumVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 拼团活动领域实体。
 * <p>
 * 这个实体描述的是交易域真正关心的活动信息快照，而不是活动域中那种更偏“运营配置管理”的完整对象。
 * 对交易域来说，它只保留“能不能下单、能下多久、成团目标是多少、是否有人群限制”等与交易直接相关的数据。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 拼团活动实体对象
 * @create 2025-01-25 12:23
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GroupBuyActivityEntity {

    /** 活动主键ID，用于唯一标识一场拼团营销活动。 */
    private Long activityId;
    /** 活动名称，主要用于展示和日志记录。 */
    private String activityName;
    /** 关联的优惠策略编号，交易域后续会根据它查询具体优惠明细。 */
    private String discountId;
    /**
     * 拼团方式。
     * 例如可以是自动成团，或必须达到目标人数后才算成团。
     * 该字段决定结算与退款时的业务分支判断。
     */
    private Integer groupType;
    /** 单个用户参与本活动的次数上限，用于控制薅羊毛和重复参加。 */
    private Integer takeLimitCount;
    /** 成团目标人数，即团队要达到多少人/单才算成功。 */
    private Integer target;
    /** 团队有效期，单位分钟，用于计算锁单截止时间和超时失效时间。 */
    private Integer validTime;
    /** 活动当前状态，只允许“生效中”的活动进入交易链路。 */
    private ActivityStatusEnumVO status;
    /** 活动生效开始时间，早于该时间一般不允许参与。 */
    private Date startTime;
    /** 活动结束时间，晚于该时间视为活动过期。 */
    private Date endTime;
    /** 人群标签ID，用于标识活动面向哪类圈选用户。 */
    private String tagId;
    /** 人群标签命中范围，如全量可见、命中可见等，用于交易前资格校验。 */
    private String tagScope;

}