package com.hjs.study.domain.trade.service;

import com.hjs.study.domain.activity.model.entity.UserGroupBuyOrderDetailEntity;
import com.hjs.study.domain.trade.model.entity.TradeRefundBehaviorEntity;
import com.hjs.study.domain.trade.model.entity.TradeRefundCommandEntity;
import com.hjs.study.domain.trade.model.valobj.TeamRefundSuccess;

import java.util.List;

/**
 * 退款逆向流程服务接口。
 * <p>
 * “逆向流程”可以理解为和正向下单、支付、结算相反的一条业务链路，
 * 主要处理退单、锁单回补、超时未支付扫描等售后与补偿动作。
 *
 * @author xiaofuge bugstack.cn @小傅哥
 * 2025/7/8 07:24
 */
public interface ITradeRefundOrderService {

    /**
     * 发起退款。
     * <p>
     * 领域服务会根据订单当前交易状态和拼团队伍状态，
     * 自动识别应走哪种退款策略。
     *
     * @param tradeRefundCommandEntity 退款命令对象
     * @return 退款行为结果，如成功、重复退款等
     */
    TradeRefundBehaviorEntity refundOrder(TradeRefundCommandEntity tradeRefundCommandEntity) throws Exception;

    /**
     * 根据退款成功消息恢复团队锁单库存。
     * <p>
     * 这个方法一般在退款成功后的通知回调链路中触发，
     * 用于把之前预占的团队名额补回去。
     *
     * @param teamRefundSuccess 退款成功消息
     */
    void restoreTeamLockStock(TeamRefundSuccess teamRefundSuccess) throws Exception;

    /**
     * 查询超时未支付订单列表
     * <p>
     * 条件一般包括：当前时间已超出活动有效范围、订单仍处于初始锁单态、尚无支付完成时间。
     * 该能力通常被定时任务调用，用于批量触发超时关闭或退款补偿。
     *
     * @return 超时未支付订单列表，限制10条
     */
    List<UserGroupBuyOrderDetailEntity> queryTimeoutUnpaidOrderList();

}
