package com.hjs.study.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 拼团队伍主单的 PO 对象。
 * <p>
 * 这个类描述的是“一个拼团队伍”的整体进度，而不是单个用户的订单明细。
 * 它保存了队伍的目标人数、已完成人数、已锁单人数、有效时间窗口以及回调配置，
 * 用于驱动成团、失败、回调通知等后续流程。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 用户拼单
 * @create 2025-01-11 10:29
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GroupBuyOrder {

    /** 数据库自增主键。 */
    private Long id;
    /** 拼团队伍唯一 ID，开团或参团时围绕这个值聚合同一支队伍。 */
    private String teamId;
    /** 当前队伍所属的拼团活动 ID。 */
    private Long activityId;
    /** 渠道标识，与 channel 组合后可唯一定位业务投放维度。 */
    private String source;
    /** 来源标识，与 source 配合使用，用于活动路由和灰度控制。 */
    private String channel;
    /** 商品原价，即未参与任何营销优惠前的价格。 */
    private BigDecimal originalPrice;
    /** 因活动优惠减免掉的金额。 */
    private BigDecimal deductionPrice;
    /** 用户最终实付价格，通常等于原价减去优惠金额。 */
    private BigDecimal payPrice;
    /** 成团目标人数，达到该人数后可进入完成态。 */
    private Integer targetCount;
    /** 已完成支付结算的人数。 */
    private Integer completeCount;
    /**
     * 已锁单人数。
     * <p>
     * 该值包含已下单但未必支付成功的成员，用于队伍名额控制，
     * 防止并发参团时出现超卖或超员。
     */
    private Integer lockCount;
    /** 队伍状态，常见值为 0 拼单中、1 完成、2 失败、3 完成但含退单。 */
    private Integer status;
    /** 队伍有效期开始时间，通常是首个成员锁单成功的时间。 */
    private Date validStartTime;
    /** 队伍有效期结束时间，超过该时间仍未成团则视为失效。 */
    private Date validEndTime;
    /** 成团后通知上游系统的方式，例如 HTTP 或 MQ。 */
    private String notifyType;
    /** HTTP 回调地址；当通知方式为 MQ 时，这个字段通常为空。 */
    private String notifyUrl;
    /** 记录创建时间。 */
    private Date createTime;
    /** 记录更新时间。 */
    private Date updateTime;

}
