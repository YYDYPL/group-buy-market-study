package com.hjs.study.domain.tag;

/**
 * 人群标签服务接口。
 * <p>
 * 该服务对外暴露标签域最核心的业务动作：
 * 按照某个标签 ID 和批次 ID，执行一次离线圈人任务。
 * 调用方通常是定时任务、批处理作业或后台运营流程。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 人群标签服务接口
 * @create 2024-12-28 11:26
 */
public interface ITagService {

    /**
     * 执行一次人群标签批次任务。
     *
     * @param tagId 标签 ID
     * @param batchId 批次 ID
     */
    void execTagBatchJob(String tagId, String batchId);

}
