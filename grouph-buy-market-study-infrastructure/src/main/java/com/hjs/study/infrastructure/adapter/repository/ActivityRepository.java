package com.hjs.study.infrastructure.adapter.repository;

import com.hjs.study.domain.activity.adapter.repository.IActivityRepository;
import com.hjs.study.domain.activity.model.entity.UserGroupBuyOrderDetailEntity;
import com.hjs.study.domain.activity.model.valobj.*;
import com.hjs.study.infrastructure.dao.*;
import com.hjs.study.infrastructure.dao.po.*;
import com.hjs.study.infrastructure.dcc.DCCService;
import com.hjs.study.infrastructure.redis.IRedisService;
import org.redisson.api.RBitSet;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 活动域仓储实现。
 * <p>
 * 该类负责把活动域所需的数据访问需求，转换为对活动表、折扣表、商品表、
 * 渠道活动关联表、拼团队伍表和人群标签缓存的具体调用。
 * 同时这里也承担了一部分“缓存优先读取”和“查询结果组装为领域 VO”的职责。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 活动仓储
 * @create 2024-12-21 10:10
 */
@Repository
public class ActivityRepository extends AbstractRepository implements IActivityRepository {

    /** 活动配置 DAO，用于读取活动基础规则。 */
    @Resource
    private IGroupBuyActivityDao groupBuyActivityDao;
    /** 折扣配置 DAO，用于读取活动关联的营销优惠方案。 */
    @Resource
    private IGroupBuyDiscountDao groupBuyDiscountDao;
    /** 商品 DAO，用于读取商品基础信息。 */
    @Resource
    private ISkuDao skuDao;
    /** 渠道商品活动关联 DAO，用于根据 source/channel/goodsId 路由到活动。 */
    @Resource
    private ISCSkuActivityDao skuActivityDao;
    /** Redis 服务，主要用于标签 BitSet 查询。 */
    @Resource
    private IRedisService redisService;
    /** DCC 动态配置中心，控制降级与切量开关。 */
    @Resource
    private DCCService dccService;
    /** 拼团队伍 DAO，用于查询队伍实时进度。 */
    @Resource
    private IGroupBuyOrderDao groupBuyOrderDao;
    /** 用户参团明细 DAO，用于查询用户进行中的拼团记录。 */
    @Resource
    private IGroupBuyOrderListDao groupBuyOrderListDao;

    /**
     * 查询活动与折扣组合后的领域视图对象。
     * <p>
     * 该方法会先读活动配置，再读折扣配置，并将两者组装成
     * {@code GroupBuyActivityDiscountVO}，供活动试算链路直接使用。
     * 活动和折扣都优先走缓存，以降低数据库读取压力。
     *
     * @param activityId 活动业务 ID
     * @return 活动折扣组合视图；未命中时返回 {@code null}
     */
    @Override
    public GroupBuyActivityDiscountVO queryGroupBuyActivityDiscountVO(Long activityId) {
        // 优先从缓存获取&写缓存，注意如果实现了后台配置，在更新时要更库，删缓存。
        GroupBuyActivity groupBuyActivityRes = getFromCacheOrDb(GroupBuyActivity.cacheRedisKey(activityId),
                () -> groupBuyActivityDao.queryValidGroupBuyActivityId(activityId));
        if (null == groupBuyActivityRes) return null;

        String discountId = groupBuyActivityRes.getDiscountId();

        // 优先从缓存获取&写缓存
        GroupBuyDiscount groupBuyDiscountRes = getFromCacheOrDb(GroupBuyDiscount.cacheRedisKey(discountId),
                () -> groupBuyDiscountDao.queryGroupBuyActivityDiscountByDiscountId(discountId));
        if (null == groupBuyDiscountRes) return null;

        // 先把优惠方案转换成内嵌值对象，便于上层按“活动 + 营销”整体读取。
        GroupBuyActivityDiscountVO.GroupBuyDiscount groupBuyDiscount = GroupBuyActivityDiscountVO.GroupBuyDiscount.builder()
                .discountName(groupBuyDiscountRes.getDiscountName())
                .discountDesc(groupBuyDiscountRes.getDiscountDesc())
                .discountType(DiscountTypeEnum.get(groupBuyDiscountRes.getDiscountType()))
                .marketPlan(groupBuyDiscountRes.getMarketPlan())
                .marketExpr(groupBuyDiscountRes.getMarketExpr())
                .tagId(groupBuyDiscountRes.getTagId())
                .build();

        return GroupBuyActivityDiscountVO.builder()
                .activityId(groupBuyActivityRes.getActivityId())
                .activityName(groupBuyActivityRes.getActivityName())
                .groupBuyDiscount(groupBuyDiscount)
                .groupType(groupBuyActivityRes.getGroupType())
                .takeLimitCount(groupBuyActivityRes.getTakeLimitCount())
                .target(groupBuyActivityRes.getTarget())
                .validTime(groupBuyActivityRes.getValidTime())
                .status(groupBuyActivityRes.getStatus())
                .startTime(groupBuyActivityRes.getStartTime())
                .endTime(groupBuyActivityRes.getEndTime())
                .tagId(groupBuyActivityRes.getTagId())
                .tagScope(groupBuyActivityRes.getTagScope())
                .build();
    }

    /**
     * 按商品 ID 查询商品视图对象。
     *
     * @param goodsId 商品业务 ID
     * @return 商品视图对象；未命中时返回 {@code null}
     */
    @Override
    public SkuVO querySkuByGoodsId(String goodsId) {
        Sku sku = skuDao.querySkuByGoodsId(goodsId);
        if (null == sku) return null;
        return SkuVO.builder()
                .goodsId(sku.getGoodsId())
                .goodsName(sku.getGoodsName())
                .originalPrice(sku.getOriginalPrice())
                .build();
    }

    /**
     * 根据渠道、来源和商品 ID 查询活动路由关系。
     *
     * @param source 渠道标识
     * @param channel 来源标识
     * @param goodsId 商品业务 ID
     * @return 渠道商品活动关系视图；未命中时返回 {@code null}
     */
    @Override
    public SCSkuActivityVO querySCSkuActivityBySCGoodsId(String source, String channel, String goodsId) {
        SCSkuActivity scSkuActivityReq = new SCSkuActivity();
        scSkuActivityReq.setSource(source);
        scSkuActivityReq.setChannel(channel);
        scSkuActivityReq.setGoodsId(goodsId);

        SCSkuActivity scSkuActivity = skuActivityDao.querySCSkuActivityBySCGoodsId(scSkuActivityReq);
        if (null == scSkuActivity) return null;

        return SCSkuActivityVO.builder()
                .source(scSkuActivity.getSource())
                .chanel(scSkuActivity.getChannel())
                .activityId(scSkuActivity.getActivityId())
                .goodsId(scSkuActivity.getGoodsId())
                .build();
    }

    /**
     * 判断用户是否命中某个人群标签。
     * <p>
     * 当前实现基于 Redis BitSet 做快速判断。
     * 如果标签 BitSet 尚不存在，系统采取“默认放行”的策略，返回 {@code true}。
     *
     * @param tagId 标签 ID
     * @param userId 用户 ID
     * @return 是否命中该标签
     */
    @Override
    public boolean isTagCrowdRange(String tagId, String userId) {
        RBitSet bitSet = redisService.getBitSet(tagId);
        // 标签位图尚未构建出来时，当前实现采用默认放行策略，避免把正常用户误拦掉。
        if (!bitSet.isExists()) return true;
        // 判断用户是否存在人群中
        return bitSet.get(redisService.getIndexFromUserId(userId));
    }

    /**
     * 查询是否开启活动域整体降级开关。
     *
     * @return {@code true} 表示开启降级
     */
    @Override
    public boolean downgradeSwitch() {
        return dccService.isDowngradeSwitch();
    }

    /**
     * 判断指定用户是否命中灰度切量范围。
     *
     * @param userId 用户 ID
     * @return {@code true} 表示命中切量范围
     */
    @Override
    public boolean cutRange(String userId) {
        return dccService.isCutRange(userId);
    }

    /**
     * 查询当前用户自己参与中的拼团明细。
     * <p>
     * 该方法会先查用户在活动下的进行中订单，再批量补齐对应队伍进度，
     * 最终组装为前端页面可展示的用户拼团详情列表。
     *
     * @param activityId 活动业务 ID
     * @param userId 用户 ID
     * @param ownerCount 最多返回条数
     * @return 用户自己参与中的拼团详情列表；无数据时返回 {@code null}
     */
    @Override
    public List<UserGroupBuyOrderDetailEntity> queryInProgressUserGroupBuyOrderDetailListByOwner(Long activityId, String userId, Integer ownerCount) {
        // 1. 根据用户ID、活动ID，查询用户参与的拼团队伍
        GroupBuyOrderList groupBuyOrderListReq = new GroupBuyOrderList();
        groupBuyOrderListReq.setActivityId(activityId);
        groupBuyOrderListReq.setUserId(userId);
        groupBuyOrderListReq.setCount(ownerCount);
        List<GroupBuyOrderList> groupBuyOrderLists = groupBuyOrderListDao.queryInProgressUserGroupBuyOrderDetailListByUserId(groupBuyOrderListReq);
        if (null == groupBuyOrderLists || groupBuyOrderLists.isEmpty()) return null;

        // 2. 过滤队伍获取 TeamId
        Set<String> teamIds = groupBuyOrderLists.stream()
                .map(GroupBuyOrderList::getTeamId)
                .filter(teamId -> teamId != null && !teamId.isEmpty()) // 过滤非空和非空字符串
                .collect(Collectors.toSet());

        // 3. 查询队伍明细，组装Map结构
        List<GroupBuyOrder> groupBuyOrders = groupBuyOrderDao.queryGroupBuyProgressByTeamIds(teamIds);
        if (null == groupBuyOrders || groupBuyOrders.isEmpty()) return null;

        Map<String, GroupBuyOrder> groupBuyOrderMap = groupBuyOrders.stream()
                .collect(Collectors.toMap(GroupBuyOrder::getTeamId, order -> order));

        // 4. 把“用户订单明细 + 队伍主单进度”拼成页面侧可直接消费的展示对象。
        List<UserGroupBuyOrderDetailEntity> userGroupBuyOrderDetailEntities = new ArrayList<>();
        for (GroupBuyOrderList groupBuyOrderList : groupBuyOrderLists) {
            String teamId = groupBuyOrderList.getTeamId();
            GroupBuyOrder groupBuyOrder = groupBuyOrderMap.get(teamId);
            if (null == groupBuyOrder) continue;

            UserGroupBuyOrderDetailEntity userGroupBuyOrderDetailEntity = UserGroupBuyOrderDetailEntity.builder()
                    .userId(groupBuyOrderList.getUserId())
                    .teamId(groupBuyOrder.getTeamId())
                    .activityId(groupBuyOrder.getActivityId())
                    .targetCount(groupBuyOrder.getTargetCount())
                    .completeCount(groupBuyOrder.getCompleteCount())
                    .lockCount(groupBuyOrder.getLockCount())
                    .validStartTime(groupBuyOrder.getValidStartTime())
                    .validEndTime(groupBuyOrder.getValidEndTime())
                    .outTradeNo(groupBuyOrderList.getOutTradeNo())
                    .build();

            userGroupBuyOrderDetailEntities.add(userGroupBuyOrderDetailEntity);
        }

        return userGroupBuyOrderDetailEntities;
    }

    /**
     * 随机查询其他用户正在进行中的拼团明细。
     * <p>
     * 典型场景是首页或详情页展示“大家都在拼”，以增强活动氛围。
     * 实现上会先查出较多候选数据，再做一次内存随机抽样。
     *
     * @param activityId 活动业务 ID
     * @param userId 当前用户 ID
     * @param randomCount 期望返回条数
     * @return 随机拼团详情列表；无数据时返回 {@code null}
     */
    @Override
    public List<UserGroupBuyOrderDetailEntity> queryInProgressUserGroupBuyOrderDetailListByRandom(Long activityId, String userId, Integer randomCount) {
        // 1. 根据用户ID、活动ID，查询用户参与的拼团队伍
        GroupBuyOrderList groupBuyOrderListReq = new GroupBuyOrderList();
        groupBuyOrderListReq.setActivityId(activityId);
        groupBuyOrderListReq.setUserId(userId);
        groupBuyOrderListReq.setCount(randomCount * 2); // 查询2倍的量，之后其中 randomCount 数量
        List<GroupBuyOrderList> groupBuyOrderLists = groupBuyOrderListDao.queryInProgressUserGroupBuyOrderDetailListByRandom(groupBuyOrderListReq);
        if (null == groupBuyOrderLists || groupBuyOrderLists.isEmpty()) return null;

        // 如果候选量足够，则在内存中打乱并截断，形成“随机展示”的效果。
        if (groupBuyOrderLists.size() > randomCount) {
            // 随机打乱列表
            Collections.shuffle(groupBuyOrderLists);
            // 获取前 randomCount 个元素
            groupBuyOrderLists = groupBuyOrderLists.subList(0, randomCount);
        }

        // 2. 过滤队伍获取 TeamId
        Set<String> teamIds = groupBuyOrderLists.stream()
                .map(GroupBuyOrderList::getTeamId)
                .filter(teamId -> teamId != null && !teamId.isEmpty()) // 过滤非空和非空字符串
                .collect(Collectors.toSet());

        // 3. 查询队伍明细，组装Map结构
        List<GroupBuyOrder> groupBuyOrders = groupBuyOrderDao.queryGroupBuyProgressByTeamIds(teamIds);
        if (null == groupBuyOrders || groupBuyOrders.isEmpty()) return null;

        Map<String, GroupBuyOrder> groupBuyOrderMap = groupBuyOrders.stream()
                .collect(Collectors.toMap(GroupBuyOrder::getTeamId, order -> order));

        // 4. 组装成前端展示所需的详情对象。
        List<UserGroupBuyOrderDetailEntity> userGroupBuyOrderDetailEntities = new ArrayList<>();
        for (GroupBuyOrderList groupBuyOrderList : groupBuyOrderLists) {
            String teamId = groupBuyOrderList.getTeamId();
            GroupBuyOrder groupBuyOrder = groupBuyOrderMap.get(teamId);
            if (null == groupBuyOrder) continue;

            UserGroupBuyOrderDetailEntity userGroupBuyOrderDetailEntity = UserGroupBuyOrderDetailEntity.builder()
                    .userId(groupBuyOrderList.getUserId())
                    .teamId(groupBuyOrder.getTeamId())
                    .activityId(groupBuyOrder.getActivityId())
                    .targetCount(groupBuyOrder.getTargetCount())
                    .completeCount(groupBuyOrder.getCompleteCount())
                    .lockCount(groupBuyOrder.getLockCount())
                    .validStartTime(groupBuyOrder.getValidStartTime())
                    .validEndTime(groupBuyOrder.getValidEndTime())
                    .build();

            userGroupBuyOrderDetailEntities.add(userGroupBuyOrderDetailEntity);
        }

        return userGroupBuyOrderDetailEntities;
    }

    /**
     * 统计某活动下的拼团整体数据。
     * <p>
     * 包括总队伍数、已完成队伍数、参与用户总量等信息，
     * 常用于活动页顶部统计展示。
     *
     * @param activityId 活动业务 ID
     * @return 队伍统计视图对象
     */
    @Override
    public TeamStatisticVO queryTeamStatisticByActivityId(Long activityId) {
        // 1. 根据活动ID查询拼团队伍
        List<GroupBuyOrderList> groupBuyOrderLists = groupBuyOrderListDao.queryInProgressUserGroupBuyOrderDetailListByActivityId(activityId);

        if (null == groupBuyOrderLists || groupBuyOrderLists.isEmpty()) {
            return new TeamStatisticVO(0, 0, 0);
        }

        // 2. 过滤队伍获取 TeamId
        Set<String> teamIds = groupBuyOrderLists.stream()
                .map(GroupBuyOrderList::getTeamId)
                .filter(teamId -> teamId != null && !teamId.isEmpty()) // 过滤非空和非空字符串
                .collect(Collectors.toSet());

        // 3. 统计数据
        Integer allTeamCount = groupBuyOrderDao.queryAllTeamCount(teamIds);
        Integer allTeamCompleteCount = groupBuyOrderDao.queryAllTeamCompleteCount(teamIds);
        Integer allTeamUserCount = groupBuyOrderDao.queryAllUserCount(teamIds);

        // 4. 返回汇总后的统计结果，供活动页展示“多少团、成团多少、多少人参与”等数据。
        return TeamStatisticVO.builder()
                .allTeamCount(allTeamCount)
                .allTeamCompleteCount(allTeamCompleteCount)
                .allTeamUserCount(allTeamUserCount)
                .build();
    }

}
