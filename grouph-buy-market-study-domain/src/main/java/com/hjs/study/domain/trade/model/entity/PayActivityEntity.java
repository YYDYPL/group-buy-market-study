package com.hjs.study.domain.trade.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 锁单阶段使用的支付活动实体。
 * <p>
 * 它是从活动域裁剪出来的一份“交易可用活动快照”，专门服务于下单/锁单场景。
 * 与 {@link GroupBuyActivityEntity} 相比，这个对象更轻，更聚焦于支付时一定要用到的字段，
 * 例如队伍编号、活动有效时间、成团目标人数等。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 拼团，支付活动实体对象
 * @create 2025-01-05 16:48
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PayActivityEntity {

    /**
     * 目标队伍 ID。
     * 为空表示当前用户要自己开新团；
     * 不为空表示当前用户要加入已有团队。
     */
    private String teamId;
    /** 当前参与的拼团活动 ID。 */
    private Long activityId;
    /** 活动名称，方便做订单快照、展示和日志追踪。 */
    private String activityName;
    /** 活动开始时间，用于判断活动是否已开始。 */
    private Date startTime;
    /** 活动结束时间，用于判断是否还能继续参与。 */
    private Date endTime;
    /** 队伍有效时长，单位分钟，用于计算预购订单和团队的截止时间。 */
    private Integer validTime;
    /** 成团目标人数/单数。 */
    private Integer targetCount;

}
