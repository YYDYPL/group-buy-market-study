package com.hjs.study.domain.tag;

/**
 * 人群标签构建与刷新领域服务。
 */
public interface ITagService {


    /**
     * 根据标签 ID 和批次 ID 执行一次标签批处理任务。
     */
    void execTagBatchJob(String tagId, String batchId);
}
