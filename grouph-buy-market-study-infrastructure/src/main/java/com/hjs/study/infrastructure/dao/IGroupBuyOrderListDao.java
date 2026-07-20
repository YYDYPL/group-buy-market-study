package com.hjs.study.infrastructure.dao;

import com.hjs.study.infrastructure.dao.po.GroupBuyOrderList;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;


/**
 * 用户参团明细 Mapper。
 * <p>
 * 该接口对应 {@code group_buy_order_list} 表，记录单个用户参与某个队伍的订单事实。
 * 它既承担锁单、结算、退单等状态变更，也承担查询用户进行中的拼团记录、
 * 统计参与次数以及扫描超时未支付订单等职责。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 用户拼单明细
 * @create 2025-01-11 09:07
 */
@Mapper
public interface IGroupBuyOrderListDao {

    /**
     * 新增一条用户参团订单明细。
     *
     * @param groupBuyOrderListReq 用户参团订单请求
     */
    void insert(GroupBuyOrderList groupBuyOrderListReq);

    /**
     * 按用户 ID + 外部交易单号查询订单明细。
     * <p>
     * 常用于锁单幂等判断，避免客户端重试时重复创建营销订单。
     *
     * @param groupBuyOrderListReq 查询条件
     * @return 命中的订单明细；未命中时返回 {@code null}
     */
    GroupBuyOrderList queryGroupBuyOrderRecordByOutTradeNo(GroupBuyOrderList groupBuyOrderListReq);

    /**
     * 统计指定用户在某活动下的参团次数。
     *
     * @param groupBuyOrderListReq 统计条件，通常包含 {@code userId} 和 {@code activityId}
     * @return 当前用户在该活动下的订单明细数
     */
    Integer queryOrderCountByActivityId(GroupBuyOrderList groupBuyOrderListReq);

    /**
     * 将订单明细状态更新为“已支付完成”。
     *
     * @param groupBuyOrderListReq 更新条件，通常包含 {@code userId}、{@code outTradeNo} 和 {@code outTradeTime}
     * @return 受影响行数，通常为 0 或 1
     */
    int updateOrderStatus2COMPLETE(GroupBuyOrderList groupBuyOrderListReq);

    /**
     * 查询某个队伍下所有已完成支付的外部交易单号。
     *
     * @param teamId 队伍 ID
     * @return 外部交易单号列表
     */
    List<String> queryGroupBuyCompleteOrderOutTradeNoListByTeamId(String teamId);

    /**
     * 查询指定用户在某活动下仍在进行中的参团记录。
     * <p>
     * 只返回状态为锁定或已支付，且活动未结束的数据。
     *
     * @param groupBuyOrderListReq 查询条件，支持通过继承的 {@code count} 控制返回条数
     * @return 用户进行中的参团明细列表
     */
    List<GroupBuyOrderList> queryInProgressUserGroupBuyOrderDetailListByUserId(GroupBuyOrderList groupBuyOrderListReq);

    /**
     * 随机查询其他用户在指定活动下进行中的拼团记录。
     * <p>
     * 该方法常用于页面展示“大家都在拼”的随机队伍数据。
     *
     * @param groupBuyOrderListReq 查询条件，包含活动 ID、当前用户 ID 和条数限制
     * @return 进行中的拼团明细列表
     */
    List<GroupBuyOrderList> queryInProgressUserGroupBuyOrderDetailListByRandom(GroupBuyOrderList groupBuyOrderListReq);

    /**
     * 查询某活动下所有进行中的拼团队伍代表明细。
     *
     * @param activityId 活动业务 ID
     * @return 该活动下的拼团明细列表
     */
    List<GroupBuyOrderList> queryInProgressUserGroupBuyOrderDetailListByActivityId(Long activityId);

    /**
     * 处理“未支付即退单”场景，将订单明细更新为退单状态。
     *
     * @param groupBuyOrderListReq 更新条件，通常包含 {@code userId} 和 {@code orderId}
     * @return 受影响行数
     */
    int unpaid2Refund(GroupBuyOrderList groupBuyOrderListReq);

    /**
     * 处理“已支付但队伍未成团”场景，将订单明细更新为退单状态。
     *
     * @param groupBuyOrderListReq 更新条件
     * @return 受影响行数
     */
    int paid2Refund(GroupBuyOrderList groupBuyOrderListReq);

    /**
     * 处理“已成团后成员退单”场景，将订单明细更新为退单状态。
     *
     * @param groupBuyOrderListReq 更新条件
     * @return 受影响行数
     */
    int paidTeam2Refund(GroupBuyOrderList groupBuyOrderListReq);

    /**
     * 扫描超时未支付订单。
     * <p>
     * 当前 SQL 条件为：状态为初始锁定、支付时间为空，并且当前时间已经超过订单结束时间。
     * 该查询通常给超时退单任务使用，单次最多返回 10 条。
     *
     * @return 超时未支付订单列表
     */
    List<GroupBuyOrderList> queryTimeoutUnpaidOrderList();

}
