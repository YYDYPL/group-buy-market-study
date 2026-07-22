package com.hjs.study.domain.trade.model.aggregate;

import com.hjs.study.domain.trade.model.entity.PayActivityEntity;
import com.hjs.study.domain.trade.model.entity.PayDiscountEntity;
import com.hjs.study.domain.trade.model.entity.UserEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 拼团锁单聚合。
 * <p>
 * 这个对象不是一张数据库表的映射，而是把一次“用户发起拼团下单”动作所需的核心业务上下文
 * 组合在一起。领域层之所以使用聚合，而不是把 {@code userId}、{@code activityId}、
 * {@code outTradeNo} 等零散参数一个个下传，是因为锁单动作本身不是单字段行为，
 * 而是一个需要同时依赖“人、活动、优惠、历史参与次数”的完整业务命令。
 * <p>
 * 仓储在处理锁单时，会基于这个聚合一次性完成：
 * 1. 判断是开新团还是加入已有团。
 * 2. 生成拼团主单/拼团子单所需的活动与优惠快照。
 * 3. 结合用户已参与次数生成业务幂等标识。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 拼团订单聚合对象；聚合可以理解用各个四肢、身体、头等组装出来一个人
 * @create 2025-01-11 10:07
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GroupBuyOrderAggregate {

    /**
     * 用户领域实体。
     * 这里承载“是谁发起本次拼团交易”的身份信息，
     * 是构建订单、校验参与资格、生成业务唯一键时的基础维度。
     */
    private UserEntity userEntity;
    /**
     * 支付活动实体。
     * 记录当前命中的拼团活动快照，例如活动编号、有效时间、目标成团人数、是否指定队伍等。
     * 锁单时要靠它决定订单有效期以及主团单的基础属性。
     */
    private PayActivityEntity payActivityEntity;
    /**
     * 支付优惠实体。
     * 保存本次下单最终采用的商品与价格快照，包括原价、优惠金额、支付金额、回调配置等。
     * 它表达的是“这笔交易最终按什么优惠规则成交”。
     */
    private PayDiscountEntity payDiscountEntity;
    /**
     * 用户已参与当前活动的次数。
     * 这个值通常由规则过滤阶段提前算好，后续用于：
     * 1. 校验是否超过活动参与上限。
     * 2. 生成业务幂等字段 {@code bizId}。
     * 3. 帮助仓储层避免重复查库。
     */
    private Integer userTakeOrderCount;

}
