package com.hjs.study.infrastructure.dao;

import com.hjs.study.infrastructure.dao.po.CrowdTagsJob;
import org.apache.ibatis.annotations.Mapper;

/**
 * 人群标签批量任务 Mapper。
 * <p>
 * 该接口用于访问 {@code crowd_tags_job} 表，读取某个标签在某个批次下的任务配置，
 * 例如统计时间窗口、统计维度与阈值规则。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 人群标签任务
 * @create 2024-12-28 11:50
 */
@Mapper
public interface ICrowdTagsJobDao {

    /**
     * 查询指定标签批次的人群任务配置。
     *
     * @param crowdTagsJobReq 查询条件，通常至少包含 {@code tagId} 和 {@code batchId}
     * @return 命中的标签任务配置；如果不存在对应批次则返回 {@code null}
     */
    CrowdTagsJob queryCrowdTagsJob(CrowdTagsJob crowdTagsJobReq);

}
