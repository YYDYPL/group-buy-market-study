package com.hjs.study.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 折扣方案配置表对应的 PO 对象。
 * <p>
 * 一个拼团活动并不直接保存优惠细节，而是通过 {@code discountId} 关联到独立的折扣配置。
 * 这里定义了优惠类型、优惠计划和表达式，领域层会再根据 {@code marketPlan}
 * 选择不同的优惠计算策略。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 折扣配置
 * @create 2024-12-07 10:06
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GroupBuyDiscount {

    /**
     * 数据库自增主键。
     */
    private Long id;

    /**
     * 折扣业务唯一 ID。
     * 业务侧配置活动时，通常通过这个字段关联折扣方案。
     */
    private String discountId;

    /**
     * 折扣标题，用于后台配置页或活动说明展示。
     */
    private String discountName;

    /**
     * 折扣描述，补充说明该优惠方案的适用方式。
     */
    private String discountDesc;

    /**
     * 折扣类型。
     * 0 表示基础优惠，所有用户都可参与；
     * 1 表示人群定向优惠，需要额外命中 tagId 对应的人群标签。
     */
    private Integer discountType;

    /**
     * 营销优惠计划编码。
     * 常见值包括：ZJ=直减、MJ=满减、N=N 元购、ZK=折扣。
     */
    private String marketPlan;

    /**
     * 营销优惠表达式。
     * 表达式格式取决于 marketPlan，例如 ZJ 可直接写减免金额，
     * MJ 通常使用“门槛,减免额”的结构。
     */
    private String marketExpr;

    /**
     * 限定优惠可见/可用的人群标签 ID。
     * 仅当 discountType 为标签定向优惠时，这个字段才真正生效。
     */
    private String tagId;

    /**
     * 记录创建时间。
     */
    private Date createTime;

    /**
     * 记录更新时间。
     */
    private Date updateTime;

    /**
     * 生成折扣配置的 Redis 缓存 Key。
     *
     * @param discountId 折扣业务 ID
     * @return Redis 中存储折扣配置的 key
     */
    public static String cacheRedisKey(String discountId) {
        return "group_buy_market_cn.bugstack.infrastructure.dao.po.GroupBuyDiscount_" + discountId;
    }

}
