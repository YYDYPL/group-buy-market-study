package com.hjs.study.infrastructure.dao;

import com.hjs.study.infrastructure.dao.po.CrowdTagsDetail;
import org.apache.ibatis.annotations.Mapper;

/**
 * 人群标签明细表 Mapper。
 * <p>
 * 该接口面向 {@code crowd_tags_detail} 表，保存“某用户命中了某个人群标签”的事实记录。
 * 它是标签过滤节点判断用户是否属于某个标签的重要数据来源。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 人群标签明细
 * @create 2024-12-28 11:49
 */
@Mapper
public interface ICrowdTagsDetailDao {

    /**
     * 新增一条用户标签归属记录。
     * <p>
     * 对应表上的唯一键为 {@code tag_id + user_id}，
     * 因此重复插入时上层通常会捕获唯一索引冲突并做幂等处理。
     *
     * @param crowdTagsDetailReq 用户标签归属请求，至少包含 {@code tagId} 与 {@code userId}
     */
    void addCrowdTagsUserId(CrowdTagsDetail crowdTagsDetailReq);

}
