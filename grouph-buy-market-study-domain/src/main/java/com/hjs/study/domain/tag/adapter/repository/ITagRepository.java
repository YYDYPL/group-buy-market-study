package com.hjs.study.domain.tag.adapter.repository;

import com.hjs.study.domain.tag.model.entity.CrowdTagsJobEntity;

/**
 * 人群标签域仓储接口。
 * <p>
 * 该接口定义标签域对外部存储层的最小依赖能力：
 * 1. 查询某个标签批次任务的规则配置；
 * 2. 把命中的用户写入标签明细；
 * 3. 回写标签总人数统计。
 * 领域服务只关心“我要做什么”，具体如何落库、是否同步 Redis BitSet，
 * 则由基础设施层实现。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 人群标签仓储接口
 * @create 2024-12-28 11:26
 */
public interface ITagRepository {

    /**
     * 查询指定标签、指定批次的计算任务配置。
     *
     * @param tagId 标签 ID
     * @param batchId 批次 ID
     * @return 标签批次任务实体；不存在时通常返回 {@code null}
     */
    CrowdTagsJobEntity queryCrowdTagsJobEntity(String tagId, String batchId);

    /**
     * 把命中的用户写入该标签的人群明细中。
     * <p>
     * 基础设施实现里通常不仅会写数据库明细表，
     * 还可能同步更新 Redis BitSet，用于后续高性能圈人判断。
     *
     * @param tagId 标签 ID
     * @param userId 命中标签的用户 ID
     */
    void addCrowdTagsUserId(String tagId, String userId);

    /**
     * 更新标签的覆盖人数统计。
     *
     * @param tagId 标签 ID
     * @param count 当前批次最终命中的人数
     */
    void updateCrowdTagsStatistics(String tagId, int count);

}
