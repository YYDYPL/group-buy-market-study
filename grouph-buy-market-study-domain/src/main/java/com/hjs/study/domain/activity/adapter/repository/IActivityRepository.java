package com.hjs.study.domain.activity.adapter.repository;

import com.hjs.study.domain.activity.model.valobj.GroupBuyActivityDiscountVO;
import com.hjs.study.domain.activity.model.valobj.SCSkuActivityVO;
import com.hjs.study.domain.activity.model.valobj.SkuVO;

import java.util.List;

/**
 * 活动域仓储接口。
 * <p>
 * 该接口站在 domain 视角定义“活动试算与活动展示需要访问哪些外部数据”。
 * 对上层服务来说，它只关心取回活动配置、商品配置、标签命中结果和拼团展示数据，
 * 不需要知道这些数据最终来自 MyBatis、Redis 还是其他系统。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 活动仓储
 * @create 2024-12-21 10:06
 */
public interface IActivityRepository {

    /**
     * 查询活动与折扣组合后的完整营销配置。
     *
     * @param activityId 活动业务 ID
     * @return 活动折扣组合值对象；未命中时返回 {@code null}
     */
    GroupBuyActivityDiscountVO queryGroupBuyActivityDiscountVO(Long activityId);

    /**
     * 按商品 ID 查询商品基础信息。
     *
     * @param goodsId 商品业务 ID
     * @return 商品值对象；未命中时返回 {@code null}
     */
    SkuVO querySkuByGoodsId(String goodsId);

    /**
     * 根据来源、渠道、商品三元组查询活动路由关系。
     *
     * @param source 渠道标识
     * @param channel 来源标识
     * @param goodsId 商品业务 ID
     * @return 渠道商品活动映射值对象；未命中时返回 {@code null}
     */
    SCSkuActivityVO querySCSkuActivityBySCGoodsId(String source, String channel, String goodsId);

    /**
     * 判断用户是否命中指定的人群标签范围。
     *
     * @param tagId 标签 ID
     * @param userId 用户 ID
     * @return {@code true} 表示命中
     */
    boolean isTagCrowdRange(String tagId, String userId);

    /**
     * 查询活动域整体降级开关。
     *
     * @return {@code true} 表示当前处于降级状态
     */
    boolean downgradeSwitch();

    /**
     * 判断用户是否命中当前灰度切量范围。
     *
     * @param userId 用户 ID
     * @return {@code true} 表示命中切量范围
     */
    boolean cutRange(String userId);

    /**
     * 查询当前用户自己参与中的拼团明细。
     *
     * @param activityId 活动 ID
     * @param userId 用户 ID
     * @param ownerCount 返回条数上限
     * @return 用户自己参与中的拼团明细列表
     */
    List<UserGroupBuyOrderDetailEntity> queryInProgressUserGroupBuyOrderDetailListByOwner(Long activityId, String userId, Integer ownerCount);

    /**
     * 随机查询其他用户正在进行中的拼团明细。
     *
     * @param activityId 活动 ID
     * @param userId 当前用户 ID
     * @param randomCount 返回条数上限
     * @return 其他用户进行中的拼团明细列表
     */
    List<UserGroupBuyOrderDetailEntity> queryInProgressUserGroupBuyOrderDetailListByRandom(Long activityId, String userId, Integer randomCount);

    /**
     * 统计活动维度下的拼团总体数据。
     *
     * @param activityId 活动 ID
     * @return 队伍统计值对象
     */
    TeamStatisticVO queryTeamStatisticByActivityId(Long activityId);

}
