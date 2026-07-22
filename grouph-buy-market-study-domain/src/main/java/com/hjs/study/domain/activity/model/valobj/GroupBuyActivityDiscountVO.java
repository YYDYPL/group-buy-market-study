package com.hjs.study.domain.activity.model.valobj;


import com.hjs.study.types.common.Constants;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.units.qual.A;

import java.util.Date;
import java.util.Objects;

/**
 * 活动与折扣组合视图对象。
 * <p>
 * 该对象是活动域最核心的配置快照之一，
 * 把活动主表配置和折扣方案配置合并到一起，方便营销试算节点一次性读取。
 * 它既包含活动是否生效、时间范围、目标人数，也包含折扣计划、标签限制等信息。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 拼团活动营销配置值对象
 * @create 2024-12-21 09:39
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GroupBuyActivityDiscountVO {

    /**
     * 活动ID
     */
    private Long activityId;
    /**
     * 活动名称
     */
    private String activityName;
    /**
     * 来源
     */
    private String source;
    /**
     * 渠道
     */
    private String channel;
    /**
     * 商品ID
     */
    private String goodsId;
    /**
     * 折扣配置
     */
    private GroupBuyDiscount groupBuyDiscount;
    /**
     * 拼团方式（0自动成团、1达成目标拼团）
     */
    private Integer groupType;
    /**
     * 拼团次数限制
     */
    private Integer takeLimitCount;
    /**
     * 拼团目标
     */
    private Integer target;
    /**
     * 拼团时长（分钟）
     */
    private Integer validTime;
    /**
     * 活动状态（0创建、1生效、2过期、3废弃）
     */
    private Integer status;
    /**
     * 活动开始时间
     */
    private Date startTime;
    /**
     * 活动结束时间
     */
    private Date endTime;
    /**
     * 人群标签规则标识
     */
    private String tagId;
    /**
     * 人群标签规则范围
     */
    private String tagScope;

    /**
     * 判断当前活动对用户是否“可见”。
     * <p>
     * `tagScope` 的第一个片段用于描述可见性限制：
     * 1 表示存在可见性标签控制；
     * 其他情况表示默认可见。
     * 这里返回的只是“规则默认值”，真正是否可见还需要与 `TagNode` 的标签命中结果结合。
     *
     * @return 是否默认可见
     */
    public boolean isVisible() {
        if (StringUtils.isBlank(this.tagScope)) return TagScopeEnumVO.VISIBLE.getAllow();
        String[] split = this.tagScope.split(Constants.SPLIT);
        if (split.length > 0 && Objects.equals(split[0], "1") && StringUtils.isNotBlank(split[0])) {
            return TagScopeEnumVO.VISIBLE.getRefuse();
        }
        return TagScopeEnumVO.VISIBLE.getAllow();
    }

    /**
     * 判断当前活动对用户是否“默认允许参与”。
     * <p>
     * `tagScope` 中的第二个片段用于描述参与限制：
     * 2 表示参与受标签控制；
     * 未命中限制配置时默认允许参与。
     *
     * @return 是否默认允许参与
     */
    public boolean isEnable() {
        if (StringUtils.isBlank(this.tagScope)) return TagScopeEnumVO.VISIBLE.getAllow();
        String[] split = this.tagScope.split(Constants.SPLIT);
        if (split.length == 2 && Objects.equals(split[1], "2") && StringUtils.isNotBlank(split[1])) {
            return TagScopeEnumVO.ENABLE.getRefuse();
        }
        if (split.length == 1 && Objects.equals(split[0], "2")) {
            return TagScopeEnumVO.ENABLE.getRefuse();
        }
        return TagScopeEnumVO.ENABLE.getAllow();
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    /**
     * 折扣配置内嵌值对象。
     * <p>
     * 之所以作为内部类存在，是因为它天然依附于活动配置视图，
     * 表示“某个活动当前挂载的那条折扣方案”。
     */
    public static class GroupBuyDiscount {
        /**
         * 折扣标题
         */
        private String discountName;

        /**
         * 折扣描述
         */
        private String discountDesc;

        /**
         * 折扣类型（0:base、1:tag）
         */
        private DiscountTypeEnum discountType;

        /** 营销优惠计划编码，如 `ZJ` 直减、`MJ` 满减、`N` N元购、`ZK` 折扣。 */
        private String marketPlan;

        /**
         * 营销优惠表达式。
         * <p>
         * 不同计划含义不同，例如：
         * `ZJ` 表示直减金额；
         * `MJ` 表示满减门槛与减免值；
         * `N` 表示固定到手价；
         * `ZK` 表示折扣系数。
         */
        private String marketExpr;

        /**
         * 人群标签，特定优惠限定
         */
        private String tagId;
    }

}
