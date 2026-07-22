package com.hjs.study.domain.trade.adapter.repository;

import com.hjs.study.domain.activity.model.entity.UserGroupBuyOrderDetailEntity;
import com.hjs.study.domain.trade.model.aggregate.GroupBuyOrderAggregate;
import com.hjs.study.domain.trade.model.aggregate.GroupBuyRefundAggregate;
import com.hjs.study.domain.trade.model.aggregate.GroupBuyTeamSettlementAggregate;
import com.hjs.study.domain.trade.model.entity.GroupBuyActivityEntity;
import com.hjs.study.domain.trade.model.entity.GroupBuyTeamEntity;
import com.hjs.study.domain.trade.model.entity.MarketPayOrderEntity;
import com.hjs.study.domain.trade.model.entity.NotifyTaskEntity;
import com.hjs.study.domain.trade.model.valobj.GroupBuyProgressVO;

import java.util.List;

/**
 * 交易域仓储接口。
 * <p>
 * 该接口定义交易域对持久化层和部分基础设施能力的统一访问入口，
 * 覆盖了拼团交易的几个关键阶段：
 * 1. 锁单与查询；
 * 2. 成团结算；
 * 3. 本地消息通知与补偿；
 * 4. 库存占用与恢复；
 * 5. 退款回滚与超时扫描。
 * <p>
 * 这里的方法虽然最终会操作多张表、Redis 和本地消息任务表，
 * 但对 domain 来说，它只暴露语义化的交易动作。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 交易仓储服务接口
 * @create 2025-01-11 09:07
 */
public interface ITradeRepository {

    /**
     * 按外部交易单号查询营销订单。
     * <p>
     * 该查询常用于下单幂等、支付回查和重复提交保护。
     *
     * @param userId 用户 ID
     * @param outTradeNo 外部交易单号
     * @return 营销支付订单实体；未命中时返回 {@code null}
     */
    MarketPayOrderEntity queryMarketPayOrderEntityByOutTradeNo(String userId, String outTradeNo);

    /**
     * 锁定一笔营销拼团订单。
     * <p>
     * 该方法是交易创建阶段的核心入口，会根据聚合对象中的上下文信息：
     * 1. 判断是自己开团还是加入已有队伍；
     * 2. 写入或更新拼团主单；
     * 3. 生成用户维度的拼团明细单；
     * 4. 返回给领域层一个与持久化解耦的营销订单视图。
     *
     * @param groupBuyOrderAggregate 拼团锁单聚合，包含用户、活动、优惠和参与次数等完整上下文
     * @return 营销支付订单实体
     */
    MarketPayOrderEntity lockMarketPayOrder(GroupBuyOrderAggregate groupBuyOrderAggregate);

    /**
     * 查询指定队伍的拼团进度。
     *
     * @param teamId 队伍 ID
     * @return 拼团进度值对象，通常包含目标人数、已完成人数、已锁定人数等信息
     */
    GroupBuyProgressVO queryGroupBuyProgress(String teamId);

    /**
     * 按活动 ID 查询交易域视角下的活动信息。
     *
     * @param activityId 活动 ID
     * @return 拼团活动实体；未命中时返回 {@code null}
     */
    GroupBuyActivityEntity queryGroupBuyActivityEntityByActivityId(Long activityId);

    /**
     * 查询指定用户在某个活动下已经参与过多少次订单。
     * <p>
     * 这个值通常参与构建业务幂等键或参与次数限制判断。
     *
     * @param activityId 活动 ID
     * @param userId 用户 ID
     * @return 用户在该活动下的参与订单数
     */
    Integer queryOrderCountByActivityId(Long activityId, String userId);

    /**
     * 查询指定队伍的当前快照信息。
     *
     * @param teamId 队伍 ID
     * @return 拼团队伍实体；未命中时返回 {@code null}
     */
    GroupBuyTeamEntity queryGroupBuyTeamByTeamId(String teamId);

    /**
     * 执行拼团队伍结算。
     * <p>
     * 通常发生在用户支付成功之后，需要更新：
     * 1. 用户订单状态；
     * 2. 队伍完成人数；
     * 3. 队伍是否已经成团；
     * 4. 是否生成后续的通知任务。
     *
     * @param groupBuyTeamSettlementAggregate 拼团队伍结算聚合
     * @return 通知任务实体；如果本次结算无需通知，则可能返回 {@code null}
     */
    NotifyTaskEntity settlementMarketPayOrder(GroupBuyTeamSettlementAggregate groupBuyTeamSettlementAggregate);

    /**
     * 判断来源和渠道组合是否被黑名单拦截。
     *
     * @param source 来源标识
     * @param channel 渠道标识
     * @return {@code true} 表示当前来源渠道组合不允许继续交易
     */
    boolean isSCBlackIntercept(String source, String channel);

    /**
     * 查询全部未执行完成的通知任务。
     * <p>
     * 常用于定时补偿任务全量扫描。
     *
     * @return 待执行通知任务列表
     */
    List<NotifyTaskEntity> queryUnExecutedNotifyTaskList();

    /**
     * 查询指定队伍下未执行完成的通知任务。
     * <p>
     * 适合在单队伍维度做补偿、去重或状态校验。
     *
     * @param teamId 队伍 ID
     * @return 待执行通知任务列表
     */
    List<NotifyTaskEntity> queryUnExecutedNotifyTaskList(String teamId);

    /**
     * 把通知任务更新为成功状态。
     *
     * @param notifyTaskEntity 通知任务实体
     * @return 实际更新行数
     */
    int updateNotifyTaskStatusSuccess(NotifyTaskEntity notifyTaskEntity);

    /**
     * 把通知任务更新为失败状态。
     *
     * @param notifyTaskEntity 通知任务实体
     * @return 实际更新行数
     */
    int updateNotifyTaskStatusError(NotifyTaskEntity notifyTaskEntity);

    /**
     * 把通知任务更新为待重试状态，并记录重试次数。
     *
     * @param notifyTaskEntity 通知任务实体
     * @return 实际更新行数
     */
    int updateNotifyTaskStatusRetry(NotifyTaskEntity notifyTaskEntity);

    /**
     * 占用队伍库存。
     * <p>
     * 该操作通常基于 Redis 完成，用于在并发锁单时先抢占一个拼团坑位，
     * 同时把可恢复库存写入另一份恢复键中，便于后续超时或退款回补。
     *
     * @param teamStockKey 队伍库存键
     * @param recoveryTeamStockKey 队伍库存恢复键
     * @param target 队伍目标人数
     * @param validTime 占用有效时间，通常与订单锁单有效期保持一致
     * @return {@code true} 表示占用成功
     */
    boolean occupyTeamStock(String teamStockKey, String recoveryTeamStockKey, Integer target, Integer validTime);

    /**
     * 恢复队伍库存。
     * <p>
     * 当订单超时未支付、退款回滚等情况发生时，
     * 需要把之前占用的库存归还到可用队伍库存中。
     *
     * @param recoveryTeamStockKey 队伍库存恢复键
     * @param validTime 恢复键有效时间
     */
    void recoveryTeamStock(String recoveryTeamStockKey, Integer validTime);

    /**
     * 将“未支付订单”回滚为退款状态。
     * <p>
     * 这种场景通常意味着只需要回退锁单人数，不需要回退已完成人数。
     *
     * @param groupBuyRefundAggregate 退款聚合
     * @return 退款后的通知任务实体
     */
    NotifyTaskEntity unpaid2Refund(GroupBuyRefundAggregate groupBuyRefundAggregate);

    /**
     * 将“已支付但队伍未成团的订单”回滚为退款状态。
     * <p>
     * 这时通常既要处理锁单数量，也要处理已支付完成数量。
     *
     * @param groupBuyRefundAggregate 退款聚合
     * @return 退款后的通知任务实体
     */
    NotifyTaskEntity paid2Refund(GroupBuyRefundAggregate groupBuyRefundAggregate);

    /**
     * 将“已成团队伍中的已支付订单”回滚为退款状态。
     * <p>
     * 这是更复杂的退款场景，除了人数回滚，还可能涉及队伍成团状态回退。
     *
     * @param groupBuyRefundAggregate 退款聚合
     * @return 退款后的通知任务实体
     */
    NotifyTaskEntity paidTeam2Refund(GroupBuyRefundAggregate groupBuyRefundAggregate);

    /**
     * 把退款成功的订单加入库存恢复集合。
     * <p>
     * 该动作通常用于异步恢复库存，避免重复恢复同一个订单。
     *
     * @param recoveryTeamStockKey 队伍库存恢复键
     * @param orderId 订单 ID
     */
    void refund2AddRecovery(String recoveryTeamStockKey, String orderId);

    /**
     * 查询超时未支付订单列表。
     * <p>
     * 常用于定时任务扫描，再结合退款与库存恢复逻辑做补偿处理。
     *
     * @return 超时未支付订单明细列表
     */
    List<UserGroupBuyOrderDetailEntity> queryTimeoutUnpaidOrderList();

}