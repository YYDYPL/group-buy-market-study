package com.hjs.study.domain.activity.service;

import com.hjs.study.domain.activity.model.entity.MarketProductEntity;
import com.hjs.study.domain.activity.model.entity.TrialBalanceEntity;
import com.hjs.study.domain.activity.model.entity.UserGroupBuyOrderDetailEntity;
import com.hjs.study.domain.activity.model.valobj.TeamStatisticVO;

import java.util.List;

/**
 * 首页拼团营销服务接口。
 * <p>
 * 该服务站在活动域对外提供三类核心能力：
 * 1. 商品拼团试算；
 * 2. 查询进行中的拼团列表；
 * 3. 查询活动整体热度统计。
 * trigger/app 层调用它时，不需要知道内部是规则树、仓储查询还是折扣策略。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 首页营销服务接口
 * @create 2024-12-14 13:39
 */
public interface IIndexGroupBuyMarketService {

    /**
     * 对指定商品执行一次拼团活动试算。
     *
     * @param marketProductEntity 试算输入参数
     * @return 试算结果实体
     * @throws Exception 试算链路中的异常统一向上抛出
     */
    TrialBalanceEntity indexMarketTrial(MarketProductEntity marketProductEntity) throws Exception;

    /**
     * 查询进行中的拼团订单
     *
     * @param activityId  活动ID
     * @param userId      用户ID
     * @param ownerCount  个人数量
     * @param randomCount 随机数量
     * @return 用户拼团明细数据
     */
    List<UserGroupBuyOrderDetailEntity> queryInProgressUserGroupBuyOrderDetailList(Long activityId, String userId, Integer ownerCount, Integer randomCount);

    /**
     * 活动拼团队伍总结
     *
     * @param activityId 活动ID
     * @return 队伍统计
     */
    TeamStatisticVO queryTeamStatisticByActivityId(Long activityId);

}
