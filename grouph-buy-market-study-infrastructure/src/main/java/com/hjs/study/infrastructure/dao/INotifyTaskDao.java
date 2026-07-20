package com.hjs.study.infrastructure.dao;

import com.hjs.study.infrastructure.dao.po.NotifyTask;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 回调通知任务 Mapper。
 * <p>
 * 该接口对应 {@code notify_task} 表，用于保存拼团完成、退单等事件对应的本地消息任务，
 * 并支持查询待执行任务与更新任务执行结果，是“事务 + 补偿通知”设计中的关键一环。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 回调任务
 * @create 2025-01-26 18:23
 */
@Mapper
public interface INotifyTaskDao {

    /**
     * 新增一条通知任务。
     *
     * @param notifyTask 通知任务数据
     */
    void insert(NotifyTask notifyTask);

    /**
     * 查询全部待执行或待重试的通知任务。
     *
     * @return 通知任务列表，当前实现只返回状态为 0 或 2 的记录
     */
    List<NotifyTask> queryUnExecutedNotifyTaskList();

    /**
     * 按队伍 ID 查询待执行或待重试的通知任务。
     *
     * @param teamId 队伍 ID
     * @return 通知任务；未命中时返回 {@code null}
     */
    NotifyTask queryUnExecutedNotifyTaskByTeamId(String teamId);

    /**
     * 将通知任务更新为执行成功。
     * <p>
     * 更新时会顺带将 notifyCount 加 1。
     *
     * @param notifyTask 更新条件，通常包含 {@code teamId} 与 {@code uuid}
     * @return 受影响行数
     */
    int updateNotifyTaskStatusSuccess(NotifyTask notifyTask);

    /**
     * 将通知任务更新为最终失败。
     * <p>
     * 更新时会顺带将 notifyCount 加 1。
     *
     * @param notifyTask 更新条件，通常包含 {@code teamId} 与 {@code uuid}
     * @return 受影响行数
     */
    int updateNotifyTaskStatusError(NotifyTask notifyTask);

    /**
     * 将通知任务更新为待重试状态。
     * <p>
     * 更新时会顺带将 notifyCount 加 1。
     *
     * @param notifyTask 更新条件，通常包含 {@code teamId} 与 {@code uuid}
     * @return 受影响行数
     */
    int updateNotifyTaskStatusRetry(NotifyTask notifyTask);

}
