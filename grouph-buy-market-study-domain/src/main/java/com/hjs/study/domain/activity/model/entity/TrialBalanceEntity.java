package com.hjs.study.domain.activity.model.entity;

import com.hjs.study.domain.activity.model.valobj.GroupBuyActivityDiscountVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 拼团活动试算结果实体。
 * <p>
 * 该实体是活动域对外返回的“试算结果快照”，
 * 它把商品信息、优惠试算结果、活动时间窗、可见性和可参与性统一封装起来，
 * 供首页、商品详情页或下单前确认页直接展示。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 试算结果实体对象（给用户展示拼团可获得的优惠信息）
 * @create 2024-12-14 13:45
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TrialBalanceEntity {

    /** 商品 ID。 */
    private String goodsId;
    /** 商品名称。 */
    private String goodsName;
    /** 商品原价。 */
    private BigDecimal originalPrice;
    /** 本次拼团可减免的金额。 */
    private BigDecimal deductionPrice;
    /** 用户实际需要支付的金额。 */
    private BigDecimal payPrice;
    /** 拼团目标人数。 */
    private Integer targetCount;
    /** 活动开始时间。 */
    private Date startTime;
    /** 活动结束时间。 */
    private Date endTime;
    /** 当前用户是否可见该拼团入口。 */
    private Boolean isVisible;
    /** 当前用户是否允许参与该拼团。 */
    private Boolean isEnable;

    /** 对应的活动折扣配置快照，便于前端展示活动文案和规则。 */
    private GroupBuyActivityDiscountVO groupBuyActivityDiscountVO;

}