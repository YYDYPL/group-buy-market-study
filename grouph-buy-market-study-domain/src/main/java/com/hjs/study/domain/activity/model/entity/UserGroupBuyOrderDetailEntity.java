package com.hjs.study.domain.activity.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 用户维度的拼团明细实体。
 * <p>
 * 该实体不是数据库 PO，而是活动域面向展示层组装出的业务结果，
 * 用于描述“某个用户参与中的某个拼团队伍现在是什么状态”。
 * 首页和活动详情页常用它来展示“我参与的团”和“大家都在拼”的列表数据。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 拼团组队实体对象
 * @create 2025-02-02 13:10
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserGroupBuyOrderDetailEntity {

    /** 用户 ID。 */
    private String userId;
    /** 拼团队伍 ID。 */
    private String teamId;
    /** 活动 ID。 */
    private Long activityId;
    /** 目标人数。 */
    private Integer targetCount;
    /** 已支付完成的人数。 */
    private Integer completeCount;
    /** 已锁定坑位的人数，包含未支付与已支付成员。 */
    private Integer lockCount;
    /** 队伍有效开始时间。 */
    private Date validStartTime;
    /** 队伍有效结束时间。 */
    private Date validEndTime;
    /** 外部交易单号，用于订单回查与幂等控制。 */
    private String outTradeNo;
    /** 来源标识。 */
    private String source;
    /** 渠道标识。 */
    private String channel;

}
