package com.hjs.study.api;

import com.hjs.study.api.dto.LockMarketPayOrderRequestDTO;
import com.hjs.study.api.dto.LockMarketPayOrderResponseDTO;
import com.hjs.study.api.dto.RefundMarketPayOrderRequestDTO;
import com.hjs.study.api.dto.RefundMarketPayOrderResponseDTO;
import com.hjs.study.api.dto.SettlementMarketPayOrderRequestDTO;
import com.hjs.study.api.dto.SettlementMarketPayOrderResponseDTO;
import com.hjs.study.api.response.Response;

/**
 * 拼团营销交易服务契约。
 *
 * <p>定义一笔营销订单从支付前锁单、支付成功结算到逆向退单的三个外部入口。接口 DTO 只描述
 * 调用协议，库存占用、订单状态流转和成团判断等规则由交易领域服务负责。</p>
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @create 2025-01-11 13:49
 */
public interface IMarketTradeService {

    /**
     * 创建或复用一笔待支付的拼团营销锁单。
     *
     * <p>同一用户与外部交易单号重复请求时应具备幂等性。请求中的队伍 ID 为空表示开新团，
     * 非空表示加入已有队伍。</p>
     *
     * @param requestDTO 用户、活动、商品、渠道、外部交易单号及通知配置
     * @return 内部订单号、服务端成交价格、交易状态和队伍 ID
     */
    Response<LockMarketPayOrderResponseDTO> lockMarketPayOrder(LockMarketPayOrderRequestDTO requestDTO);

    /**
     * 接收外部支付成功事实并结算营销订单。
     *
     * <p>该接口不负责实际收款，而是根据外部交易单号和支付时间推进订单及队伍状态。</p>
     *
     * @param requestDTO 支付来源、渠道、用户、外部交易单号和支付时间
     * @return 结算后的用户、队伍、活动及外部交易标识
     */
    Response<SettlementMarketPayOrderResponseDTO> settlementMarketPayOrder(SettlementMarketPayOrderRequestDTO requestDTO);

    /**
     * 申请撤销指定的拼团营销订单。
     *
     * <p>领域层会根据订单当前处于未支付、已支付或已成团状态选择不同退单策略。</p>
     *
     * @param requestDTO 用于定位订单的用户、外部交易单号和业务渠道上下文
     * @return 被处理的订单、队伍以及实际执行的退单行为
     */
    Response<RefundMarketPayOrderResponseDTO> refundMarketPayOrder(RefundMarketPayOrderRequestDTO requestDTO);

}
