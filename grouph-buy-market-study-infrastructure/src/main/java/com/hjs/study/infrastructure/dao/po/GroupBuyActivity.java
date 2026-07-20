package com.hjs.study.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 拼团活动配置表对应的 PO 对象。
 * <p>
 * 该对象描述一个活动本身的核心规则，包括活动名称、关联折扣、成团方式、
 * 成团目标人数、活动有效时间，以及是否需要做人群可见/参与限制。
 * 应用层和领域层查询活动信息时，最终落到数据库映射的就是这个对象。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 拼团活动
 * @create 2024-12-07 10:01
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GroupBuyActivity {

    /** 数据库自增主键。 */
    private Long id;
    /** 活动业务唯一 ID，对外传递和缓存定位通常都使用该字段。 */
    private Long activityId;
    /** 活动名称，主要用于页面展示与后台配置识别。 */
    private String activityName;
    /** 关联的折扣方案 ID，对应 {@code group_buy_discount.discount_id}。 */
    private String discountId;
    /** 拼团方式，0 表示自动成团，1 表示达到目标人数后成团。 */
    private Integer groupType;
    /** 单个用户在当前活动下允许参与拼团的最大次数。 */
    private Integer takeLimitCount;
    /** 成团目标人数，即 completeCount 达到该值时视为成团成功。 */
    private Integer target;
    /** 队伍有效时长，单位分钟，从首单锁定成功开始计时。 */
    private Integer validTime;
    /** 活动状态：0 创建、1 生效、2 过期、3 废弃。 */
    private Integer status;
    /** 活动全局开始时间，早于该时间不允许参与。 */
    private Date startTime;
    /** 活动全局结束时间，晚于该时间视为活动已结束。 */
    private Date endTime;
    /**
     * 活动人群标签 ID。
     * <p>
     * 为空表示活动对所有用户开放；不为空时，系统会结合 tagScope 判断
     * 用户是否有查看权限或参与资格。
     */
    private String tagId;
    /**
     * 标签限制范围。
     * <p>
     * 常见取值：1 表示仅限制可见、2 表示仅限制参与、1,2 表示两者都限制。
     */
    private String tagScope;
    /** 记录创建时间。 */
    private Date createTime;
    /** 记录更新时间。 */
    private Date updateTime;

    /**
     * 生成活动缓存使用的 Redis Key。
     *
     * @param activityId 活动业务 ID
     * @return Redis 中缓存活动配置的 key
     */
    public static String cacheRedisKey(Long activityId) {
        return "group_buy_market_cn.bugstack.infrastructure.dao.po.GroupBuyActivity_" + activityId;
    }

}

