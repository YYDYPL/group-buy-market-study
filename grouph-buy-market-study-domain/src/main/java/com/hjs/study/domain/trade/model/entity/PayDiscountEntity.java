package com.hjs.study.domain.trade.model.entity;

import com.hjs.study.domain.trade.model.valobj.NotifyConfigVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 锁单阶段使用的支付优惠实体。
 * <p>
 * 它描述的是“这次下单按什么商品和什么价格成交”，
 * 本质上是一份交易价格快照。之所以要单独抽成实体，
 * 是因为活动信息和价格信息虽然都服务于下单，但职责并不相同：
 * 活动控制资格与时间，优惠控制商品与金额。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 拼团，支付优惠实体对象
 * @create 2025-01-05 16:46
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PayDiscountEntity {

    /** 业务来源，例如来自哪个投放源或接入系统。 */
    private String source;
    /** 渠道标识，用于区分具体入口、端或场景。 */
    private String channel;
    /** 本次交易对应的商品 ID。 */
    private String goodsId;
    /** 商品名称，用于订单快照和后续通知展示。 */
    private String goodsName;
    /** 商品原价，即未参与任何优惠时的价格。 */
    private BigDecimal originalPrice;
    /** 优惠让利金额。 */
    private BigDecimal deductionPrice;
    /** 用户最终应付金额。 */
    private BigDecimal payPrice;
    /**
     * 外部交易单号。
     * 它通常由接入方传入，用于确保一次外部支付请求在系统内只落一笔有效交易，
     * 是非常重要的幂等锚点。
     */
    private String outTradeNo;
    /** 结算或退款成功后应该如何通知外部系统的配置。 */
    private NotifyConfigVO notifyConfigVO;

}
