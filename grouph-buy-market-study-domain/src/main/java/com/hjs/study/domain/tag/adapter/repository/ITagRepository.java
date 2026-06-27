package com.hjs.study.domain.tag.adapter.repository;

import com.hjs.study.domain.tag.model.entity.CrowdTagsJobEntity;

/**
 * 标签领域服务使用的仓储端口。
 */
public interface ITagRepository {

    /**
     * 查询人群标签批处理任务元数据。
     */
    CrowdTagsJobEntity queryCrowdTagsJobEntity(String tagId, String batchId);

    /**
     * 将一个用户写入人群标签明细集合。
     */
    void addCrowdTagsUserId(String tagId, String userId);

    /**
     * 刷新人群标签聚合统计值。
     */
    void updateCrowdTagsStatistics(String tagId, int count);

}
