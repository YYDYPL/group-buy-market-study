package com.hjs.study.infrastructure.dao;

import com.hjs.study.infrastructure.dao.po.GroupBuyOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Set;

/**
 * 拼团队伍主单 Mapper。
 * <p>
 * 该接口对应 {@code group_buy_order} 表，记录的是“队伍级别”的聚合状态，
 * 例如目标人数、已完成支付人数、已锁单人数、队伍状态以及回调配置。
 * 锁单、结算、退单、补偿通知等主流程都会调用这里的方法。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 用户拼单
 * @create 2025-01-11 10:33
 */
@Mapper
public interface IGroupBuyOrderDao {

    /**
     * 新增一条拼团队伍主单记录。
     *
     * @param groupBuyOrder 队伍主单数据
     */
    void insert(GroupBuyOrder groupBuyOrder);

    /**
     * 为指定队伍增加 1 个锁单人数。
     * <p>
     * SQL 层带有 {@code lock_count < target_count} 保护，
     * 用于避免队伍人数超过目标人数。
     *
     * @param teamId 队伍 ID
     * @return 受影响行数，通常为 0 或 1
     */
    int updateAddLockCount(String teamId);

    /**
     * 为指定队伍减少 1 个锁单人数。
     *
     * @param teamId 队伍 ID
     * @return 受影响行数，通常为 0 或 1
     */
    int updateSubtractionLockCount(String teamId);

    /**
     * 查询队伍当前进度信息。
     * <p>
     * 主要返回目标人数、已完成人数和锁单人数，用于判断能否继续参团或是否已成团。
     *
     * @param teamId 队伍 ID
     * @return 队伍进度对象；不存在时返回 {@code null}
     */
    GroupBuyOrder queryGroupBuyProgress(String teamId);

    /**
     * 按队伍 ID 查询队伍主单信息。
     *
     * @param teamId 队伍 ID
     * @return 队伍主单；不存在时返回 {@code null}
     */
    GroupBuyOrder queryGroupBuyTeamByTeamId(String teamId);

    /**
     * 批量查询队伍主单信息。
     *
     * @param teamIds 队伍 ID 集合
     * @return 命中的队伍主单列表
     */
    List<GroupBuyOrder> queryGroupBuyTeamByTeamIds(@Param("teamIds") Set<String> teamIds);

    /**
     * 为指定队伍增加 1 个已完成支付人数。
     * <p>
     * SQL 层带有 {@code complete_count < target_count} 保护。
     *
     * @param teamId 队伍 ID
     * @return 受影响行数，通常为 0 或 1
     */
    int updateAddCompleteCount(String teamId);

    /**
     * 将队伍状态从“拼单中”更新为“已完成”。
     *
     * @param teamId 队伍 ID
     * @return 受影响行数，通常为 0 或 1
     */
    int updateOrderStatus2COMPLETE(String teamId);

    /**
     * 批量查询仍在进行中的队伍。
     * <p>
     * 只返回状态为进行中、未满员且未超时的队伍。
     *
     * @param teamIds 队伍 ID 集合
     * @return 进行中的队伍列表
     */
    List<GroupBuyOrder> queryGroupBuyProgressByTeamIds(@Param("teamIds") Set<String> teamIds);

    /**
     * 统计指定队伍集合的总队伍数。
     *
     * @param teamIds 队伍 ID 集合
     * @return 队伍总数
     */
    Integer queryAllTeamCount(@Param("teamIds") Set<String> teamIds);

    /**
     * 统计指定队伍集合中已成团的队伍数量。
     *
     * @param teamIds 队伍 ID 集合
     * @return 已完成队伍数
     */
    Integer queryAllTeamCompleteCount(@Param("teamIds") Set<String> teamIds);

    /**
     * 汇总指定队伍集合的锁单人数。
     *
     * @param teamIds 队伍 ID 集合
     * @return 锁单人数总和
     */
    Integer queryAllUserCount(@Param("teamIds") Set<String> teamIds);

    Integer queryAllTeamCountByActivityId(Long activityId);

    Integer queryAllTeamCompleteCountByActivityId(Long activityId);

    /**
     * 处理“未支付即退单”场景下的队伍主单回滚。
     *
     * @param groupBuyOrderReq 队伍更新参数
     * @return 受影响行数
     */
    int unpaid2Refund(GroupBuyOrder groupBuyOrderReq);

    /**
     * 处理“已支付但队伍未成团”场景下的队伍主单回滚。
     *
     * @param groupBuyOrderReq 队伍更新参数
     * @return 受影响行数
     */
    int paid2Refund(GroupBuyOrder groupBuyOrderReq);

    /**
     * 处理“已成团后成员退单”场景，将队伍标记为完成但含退单。
     *
     * @param groupBuyOrderReq 队伍更新参数
     * @return 受影响行数
     */
    int paidTeam2Refund(GroupBuyOrder groupBuyOrderReq);

    /**
     * 处理“已成团后最后一个有效成员退单”场景，将队伍标记为失败。
     *
     * @param groupBuyOrderReq 队伍更新参数
     * @return 受影响行数
     */
    int paidTeam2RefundFail(GroupBuyOrder groupBuyOrderReq);

}
