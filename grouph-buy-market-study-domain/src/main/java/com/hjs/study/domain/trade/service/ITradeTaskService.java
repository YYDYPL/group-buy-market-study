package com.hjs.study.domain.trade.service;

import com.hjs.study.domain.trade.model.entity.NotifyTaskEntity;

import java.util.Map;

/**
 * 交易通知任务服务接口。
 * <p>
 * 交易域把成团通知、退款通知等动作抽象成“任务”，
 * 再由该接口统一触发执行。这样可以把主交易流程和外部回调解耦，
 * 便于失败重试与补偿调度。
 *
 * @author xiaofuge bugstack.cn @小傅哥
 * 2025/7/12 21:15
 */
public interface ITradeTaskService {

    /**
     * 执行全部待处理的通知任务。
     *
     * @return 执行统计结果，如成功数、失败数、重试数
     */
    Map<String, Integer> execNotifyJob() throws Exception;

    /**
     * 执行指定团队的通知任务。
     * <p>
     * 常用于手工补偿或单团队重放通知。
     *
     * @param teamId 指定结算组ID
     * @return 执行统计结果
     */
    Map<String, Integer> execNotifyJob(String teamId) throws Exception;

    /**
     * 直接执行一条指定的通知任务。
     * <p>
     * 适合在结算或退款完成后，立即异步触发一次通知，
     * 同时保留后续失败重试能力。
     *
     * @param notifyTaskEntity 通知任务对象
     * @return 执行统计结果
     */
    Map<String, Integer> execNotifyJob(NotifyTaskEntity notifyTaskEntity) throws Exception;

}
