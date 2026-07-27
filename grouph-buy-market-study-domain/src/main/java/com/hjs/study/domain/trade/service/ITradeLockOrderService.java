package com.hjs.study.domain.trade.service;

import com.hjs.study.domain.trade.model.entity.MarketPayOrderEntity;
import com.hjs.study.domain.trade.model.entity.PayActivityEntity;
import com.hjs.study.domain.trade.model.entity.PayDiscountEntity;
import com.hjs.study.domain.trade.model.entity.UserEntity;
import com.hjs.study.domain.trade.model.entity.GroupBuyTeamEntity;
import com.hjs.study.domain.trade.model.valobj.GroupBuyProgressVO;

/**
 * 拼团交易锁单服务接口。
 * <p>
 * 这一层站在应用层看来，是“下单前预锁定拼团资格与订单”的统一入口。
 * 它负责暴露三个核心能力：
 * 1. 查询外部单号对应的未支付预购订单，便于幂等处理。
 * 2. 查询当前拼团队伍进度，便于前端或上层展示成团状态。
 * 3. 执行锁单，完成下单前的资格校验、占位和预购订单生成。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 拼团交易锁单服务接口
 * @create 2025-01-05 16:42
 */
public interface ITradeLockOrderService {

    /**
     * 根据用户 ID 与外部交易单号查询尚未支付完成的预购订单。
     * <p>
     * 这个方法常用于防重复下单或支付前查询，
     * 目的是在外部系统重复调用时快速拿到已存在的预锁订单结果。
     *
     * @param userId     用户ID
     * @param outTradeNo 外部唯一单号
     * @return 拼团预购订单结果实体
     */
    MarketPayOrderEntity queryNoPayMarketPayOrderByOutTradeNo(String userId, String outTradeNo);

    /**
     * 查询指定拼团队伍的当前进度。
     * <p>
     * 返回值通常包含目标数、完成数、锁单数，用于判断：
     * 当前是否还在拼团中、距离成团还差多少、是否已有成员占位。
     *
     * @param teamId 拼团ID
     * @return 拼团进度值对象
     */
    GroupBuyProgressVO queryGroupBuyProgress(String teamId);

    /**
     * 查询指定团队的完整状态，供参团前校验使用。
     */
    GroupBuyTeamEntity queryGroupBuyTeamByTeamId(String teamId);

    /**
     * 锁定营销预购订单。
     * <p>
     * 这是正式支付前的关键一步，通常会先做活动有效性、用户参与次数、团队库存占用等规则检查，
     * 再生成内部预购订单和拼团队伍占位信息。
     *
     * @param userEntity        交易域裁剪后的用户实体
     * @param payActivityEntity 下单场景所需的活动快照
     * @param payDiscountEntity 下单场景所需的优惠与价格快照
     * @return 锁单成功后的预购订单结果
     */
    MarketPayOrderEntity lockMarketPayOrder(UserEntity userEntity, PayActivityEntity payActivityEntity, PayDiscountEntity payDiscountEntity) throws Exception;

}
