package com.hjs.study.infrastructure.dao;

import com.hjs.study.infrastructure.dao.po.GroupBuyActivity;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 拼团活动Dao
 * @create 2024-12-07 10:10
 */
@Mapper
public interface IGroupBuyActivityDao {


    /**
     * 查询全部拼团活动配置。
     *
     * @return 活动列表，通常用于测试、后台查看或初始化类场景
     */
    List<GroupBuyActivity> queryGroupBuyActivityList();

    /**
     * 按请求条件查询一条活动配置。
     * <p>
     * 当前 XML 中是按 {@code source + channel} 取最新一条记录，
     * 更像是渠道维度的活动路由查询。
     *
     * @param groupBuyActivityReq 活动查询条件
     * @return 命中的活动配置；未命中时返回 {@code null}
     */
    GroupBuyActivity queryValidGroupBuyActivity(GroupBuyActivity groupBuyActivityReq);

    /**
     * 查询处于生效状态的活动配置。
     *
     * @param activityId 活动业务 ID
     * @return 状态为生效的活动配置；若活动不存在或未生效则返回 {@code null}
     */
    GroupBuyActivity queryValidGroupBuyActivityId(Long activityId);

    /**
     * 按活动业务 ID 查询活动配置，不额外校验状态。
     *
     * @param activityId 活动业务 ID
     * @return 活动配置；未命中时返回 {@code null}
     */
    GroupBuyActivity queryGroupBuyActivityByActivityId(Long activityId);

}
