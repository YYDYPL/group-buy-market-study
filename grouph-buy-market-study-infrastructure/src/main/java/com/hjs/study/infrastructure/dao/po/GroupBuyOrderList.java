package com.hjs.study.infrastructure.dao.po;

import com.hjs.study.infrastructure.dao.po.base.Page;
import lombok.*;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 用户参团明细表对应的 PO 对象。
 * <p>
 * 与 {@link GroupBuyOrder} 保存“队伍整体进度”不同，这个类记录的是“单个用户参与某个队伍”
 * 的订单事实，包括锁单、支付、退单以及外部交易单号等信息。
 * 同时它继承了 {@link Page}，以便在 DAO 查询场景中携带数量限制参数。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 用户拼单明细
 * @create 2025-01-11 08:42
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GroupBuyOrderList extends Page {

    /** 数据库自增主键。 */
    private Long id;
    /** 参与当前拼团的用户 ID。 */
    private String userId;
    /** 当前用户所属的拼团队伍 ID。 */
    private String teamId;
    /** 营销系统内部生成的订单号，和外部交易单号不是同一个概念。 */
    private String orderId;
    /** 参团对应的活动 ID。 */
    private Long activityId;
    /** 活动开始时间，写入明细用于后续链路快速判断活动有效性。 */
    private Date startTime;
    /** 活动结束时间。 */
    private Date endTime;
    /** 本次参与拼团的商品 ID。 */
    private String goodsId;
    /** 渠道标识。 */
    private String source;
    /** 来源标识。 */
    private String channel;
    /** 商品原价。 */
    private BigDecimal originalPrice;
    /** 优惠减免金额。 */
    private BigDecimal deductionPrice;
    /** 用户实际支付金额。 */
    private BigDecimal payPrice;
    /** 明细状态：0 初始锁定、1 消费完成、2 用户退单。 */
    private Integer status;
    /**
     * 外部交易单号。
     * <p>
     * 该值由上游系统传入，用于和外部订单/支付系统关联，
     * 同时承担接口幂等校验的重要职责。
     */
    private String outTradeNo;
    /** 外部交易完成时间，通常在支付结算成功后写入。 */
    private Date outTradeTime;
    /**
     * 业务唯一 ID。
     * <p>
     * 常见构成为 {@code activityId_userId_takeLimitCount}，
     * 用于表达“某个用户在某活动下的第几次参与”，便于业务防重。
     */
    private String bizId;
    /** 记录创建时间。 */
    private Date createTime;
    /** 记录更新时间。 */
    private Date updateTime;

}