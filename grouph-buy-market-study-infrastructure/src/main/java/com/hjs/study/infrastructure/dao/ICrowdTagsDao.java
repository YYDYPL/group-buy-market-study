package com.hjs.study.infrastructure.dao;

import com.hjs.study.infrastructure.dao.po.CrowdTags;
import org.apache.ibatis.annotations.Mapper;

/**
 * 人群标签主表 Mapper。
 * <p>
 * 该接口主要面向 {@code crowd_tags} 表，用于维护标签定义本身的统计信息。
 * 当前只暴露了一个更新方法，用于在批量圈选用户完成后回写标签覆盖人数。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 人群标签
 * @create 2024-12-28 11:49
 */
@Mapper
public interface ICrowdTagsDao {

    /**
     * 累加标签覆盖人数统计。
     * <p>
     * 该方法不会直接覆盖 statistics，而是执行
     * {@code statistics = statistics + #{statistics}} 的累加更新。
     *
     * @param crowdTagsReq 标签更新请求对象，至少需要提供 {@code tagId} 和本次要累加的 {@code statistics}
     */
    void updateCrowdTagsStatistics(CrowdTags crowdTagsReq);

}
