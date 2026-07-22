package com.hjs.study.domain.trade.model.entity;

import com.hjs.study.domain.trade.model.valobj.TradeOrderStatusEnumVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 拼团预购订单结果实体。
 * <p>
 * 这个实体通常作为锁单成功后的返回结果给应用层或接口层使用，
 * 它不是完整订单明细，而是“本次营销交易生成了什么订单结果”的轻量表达。
 * 前端或上层服务一般只关心订单号、团队号、价格以及当前交易状态即可。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 拼团，预购订单营销实体对象
 * @create 2025-01-05 16:53
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MarketPayOrderEntity {

    /** 所属拼团队伍编号，便于后续查询拼团进度。 */
    private String teamId;
    /** 内部生成的预购订单号，是后续支付、退款、查询的核心索引。 */
    private String orderId;
    /** 商品原始售价，即不参与拼团优惠时的价格。 */
    private BigDecimal originalPrice;
    /** 本次营销让利金额，用于表达活动节省了多少钱。 */
    private BigDecimal deductionPrice;
    /** 用户最终应支付金额。 */
    private BigDecimal payPrice;
    /** 当前订单交易状态，如创建、已支付完成、已关闭。 */
    private TradeOrderStatusEnumVO tradeOrderStatusEnumVO;

}
