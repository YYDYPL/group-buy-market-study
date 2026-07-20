package com.hjs.study.infrastructure.adapter.repository;

import com.hjs.study.domain.activity.model.entity.UserGroupBuyOrderDetailEntity;
import com.hjs.study.domain.trade.adapter.repository.ITradeRepository;
import com.hjs.study.domain.trade.model.aggregate.GroupBuyOrderAggregate;
import com.hjs.study.domain.trade.model.aggregate.GroupBuyRefundAggregate;
import com.hjs.study.domain.trade.model.aggregate.GroupBuyTeamSettlementAggregate;
import com.hjs.study.domain.trade.model.entity.*;
import com.hjs.study.domain.trade.model.valobj.*;
import com.hjs.study.infrastructure.dao.IGroupBuyActivityDao;
import com.hjs.study.infrastructure.dao.IGroupBuyOrderDao;
import com.hjs.study.infrastructure.dao.IGroupBuyOrderListDao;
import com.hjs.study.infrastructure.dao.INotifyTaskDao;
import com.hjs.study.infrastructure.dao.po.GroupBuyActivity;
import com.hjs.study.infrastructure.dao.po.GroupBuyOrder;
import com.hjs.study.infrastructure.dao.po.GroupBuyOrderList;
import com.hjs.study.infrastructure.dao.po.NotifyTask;
import com.hjs.study.infrastructure.dcc.DCCService;
import com.hjs.study.infrastructure.redis.IRedisService;
import com.hjs.study.types.common.Constants;
import com.hjs.study.types.enums.ActivityStatusEnumVO;
import com.hjs.study.types.enums.GroupBuyOrderEnumVO;
import com.hjs.study.types.enums.ResponseCode;
import com.hjs.study.types.exception.AppException;
import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 交易域仓储实现。
 * <p>
 * 该类是整个拼团交易链路最核心的基础设施适配器之一，负责把 domain 层的交易聚合操作，
 * 映射为对活动表、队伍主单、用户明细单、通知任务表、Redis 库存和 DCC 配置的具体读写。
 * 从正向锁单、支付结算，到逆向退单、库存恢复、超时补偿，基本都由这里落地。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 交易仓储服务
 * @create 2025-01-11 09:17
 */
@Slf4j
@Repository
public class TradeRepository implements ITradeRepository {

    /** 活动配置 DAO，用于在交易链路中回查活动基础规则。 */
    @Resource
    private IGroupBuyActivityDao groupBuyActivityDao;
    /** 拼团队伍主单 DAO，负责队伍维度的状态与人数维护。 */
    @Resource
    private IGroupBuyOrderDao groupBuyOrderDao;
    /** 用户参团明细 DAO，负责用户维度的锁单、结算、退单记录。 */
    @Resource
    private IGroupBuyOrderListDao groupBuyOrderListDao;
    /** 本地通知任务 DAO，用于落地最终一致性消息任务。 */
    @Resource
    private INotifyTaskDao notifyTaskDao;
    /** DCC 动态配置中心，用于黑名单、降级等运行时控制。 */
    @Resource
    private DCCService dccService;

    /** 拼团成功后的通知路由键。 */
    @Value("${spring.rabbitmq.config.producer.topic_team_success.routing_key}")
    private String topic_team_success;

    /** 拼团退款后的通知路由键。 */
    @Value("${spring.rabbitmq.config.producer.topic_team_refund.routing_key}")
    private String topic_team_refund;

    /** Redis 服务，承担库存占用、补偿恢复、幂等锁等能力。 */
    @Resource
    private IRedisService redisService;

    /**
     * 按用户与外部交易单号查询营销侧支付订单信息。
     * <p>
     * 典型用途是锁单幂等判断或支付后回查，防止同一个外部单号被重复处理。
     *
     * @param userId 用户 ID
     * @param outTradeNo 外部交易单号
     * @return 营销支付订单实体；未命中时返回 {@code null}
     */
    @Override
    public MarketPayOrderEntity queryMarketPayOrderEntityByOutTradeNo(String userId, String outTradeNo) {
        GroupBuyOrderList groupBuyOrderListReq = new GroupBuyOrderList();
        groupBuyOrderListReq.setUserId(userId);
        groupBuyOrderListReq.setOutTradeNo(outTradeNo);
        GroupBuyOrderList groupBuyOrderListRes = groupBuyOrderListDao.queryGroupBuyOrderRecordByOutTradeNo(groupBuyOrderListReq);
        if (null == groupBuyOrderListRes) return null;

        return MarketPayOrderEntity.builder()
                .teamId(groupBuyOrderListRes.getTeamId())
                .orderId(groupBuyOrderListRes.getOrderId())
                .originalPrice(groupBuyOrderListRes.getOriginalPrice())
                .deductionPrice(groupBuyOrderListRes.getDeductionPrice())
                .payPrice(groupBuyOrderListRes.getPayPrice())
                .tradeOrderStatusEnumVO(TradeOrderStatusEnumVO.valueOf(groupBuyOrderListRes.getStatus()))
                .build();
    }

    @Transactional(timeout = 500)
    /**
     * 锁定一笔拼团营销订单。
     * <p>
     * 该方法会根据是否携带 teamId 判断当前是“开新团”还是“加入已有团”：
     * 新开团时创建队伍主单；
     * 参团时增加已有队伍的锁单人数。
     * 随后再统一写入用户参团明细，最终返回营销侧订单结果。
     *
     * @param groupBuyOrderAggregate 锁单聚合对象
     * @return 锁单后的营销订单实体
     */
    @Override
    public MarketPayOrderEntity lockMarketPayOrder(GroupBuyOrderAggregate groupBuyOrderAggregate) {
        // 聚合对象信息
        UserEntity userEntity = groupBuyOrderAggregate.getUserEntity();
        PayActivityEntity payActivityEntity = groupBuyOrderAggregate.getPayActivityEntity();
        PayDiscountEntity payDiscountEntity = groupBuyOrderAggregate.getPayDiscountEntity();
        NotifyConfigVO notifyConfigVO = payDiscountEntity.getNotifyConfigVO();
        Integer userTakeOrderCount = groupBuyOrderAggregate.getUserTakeOrderCount();

        // 判断是否有团 - teamId 为空表示“自己开团”，不为空表示“加入已有队伍”。
        String teamId = payActivityEntity.getTeamId();
        if (StringUtils.isBlank(teamId)) {
            // 使用 RandomStringUtils.randomNumeric 替代公司里使用的雪花算法UUID
            teamId = RandomStringUtils.randomNumeric(8);

            // 新团时要先创建一条队伍主单，lockCount 初始为 1，表示当前下单用户已经占了一个坑位。
            GroupBuyOrder groupBuyOrder = GroupBuyOrder.builder()
                    .teamId(teamId)
                    .activityId(payActivityEntity.getActivityId())
                    .source(payDiscountEntity.getSource())
                    .channel(payDiscountEntity.getChannel())
                    .originalPrice(payDiscountEntity.getOriginalPrice())
                    .deductionPrice(payDiscountEntity.getDeductionPrice())
                    .payPrice(payDiscountEntity.getPayPrice())
                    .targetCount(payActivityEntity.getTargetCount())
                    .completeCount(0)
                    .lockCount(1)
                    .validStartTime(payActivityEntity.getStartTime())
                    .validEndTime(payActivityEntity.getEndTime())
                    .notifyType(notifyConfigVO.getNotifyType().getCode())
                    .notifyUrl(notifyConfigVO.getNotifyUrl())
                    .build();

            // 写入记录
            groupBuyOrderDao.insert(groupBuyOrder);
        } else {
            // 老团时只做锁单人数增加；更新不到 1 行，通常意味着团已满或状态不允许继续参团。
            int updateAddTargetCount = groupBuyOrderDao.updateAddLockCount(teamId);
            if (1 != updateAddTargetCount) {
                throw new AppException(ResponseCode.E0005);
            }
        }

        // 日期处理
        Date currentDate = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentDate);
        calendar.add(Calendar.MINUTE, payActivityEntity.getValidTime());

        // 明细单是“用户级事实订单”，无论新团还是老团，最终都要落一条 group_buy_order_list 记录。
        String orderId = RandomStringUtils.randomNumeric(12);
        GroupBuyOrderList groupBuyOrderListReq = GroupBuyOrderList.builder()
                .userId(userEntity.getUserId())
                .teamId(teamId)
                .orderId(orderId)
                .activityId(payActivityEntity.getActivityId())
                .startTime(currentDate)
                .endTime(calendar.getTime())
                .goodsId(payDiscountEntity.getGoodsId())
                .source(payDiscountEntity.getSource())
                .channel(payDiscountEntity.getChannel())
                .originalPrice(payDiscountEntity.getOriginalPrice())
                .deductionPrice(payDiscountEntity.getDeductionPrice())
                .payPrice(payDiscountEntity.getPayPrice())
                .status(TradeOrderStatusEnumVO.CREATE.getCode())
                .outTradeNo(payDiscountEntity.getOutTradeNo())
                // 构建 bizId 唯一值；活动id_用户id_参与次数累加
                .bizId(payActivityEntity.getActivityId() + Constants.UNDERLINE + userEntity.getUserId() + Constants.UNDERLINE + (userTakeOrderCount + 1))
                .build();
        try {
            // 写入拼团记录
            groupBuyOrderListDao.insert(groupBuyOrderListReq);
        } catch (DuplicateKeyException e) {
            throw new AppException(ResponseCode.INDEX_EXCEPTION);
        }

        // 返回给上层的是营销订单视图，而不是数据库 PO，这样 domain 不需要感知持久化模型。
        return MarketPayOrderEntity.builder()
                .orderId(orderId)
                .originalPrice(payDiscountEntity.getOriginalPrice())
                .deductionPrice(payDiscountEntity.getDeductionPrice())
                .payPrice(payDiscountEntity.getPayPrice())
                .tradeOrderStatusEnumVO(TradeOrderStatusEnumVO.CREATE)
                .teamId(teamId)
                .build();
    }

    /**
     * 查询某个队伍当前的拼团进度。
     *
     * @param teamId 队伍 ID
     * @return 队伍进度值对象；未命中时返回 {@code null}
     */
    @Override
    public GroupBuyProgressVO queryGroupBuyProgress(String teamId) {
        GroupBuyOrder groupBuyOrder = groupBuyOrderDao.queryGroupBuyProgress(teamId);
        if (null == groupBuyOrder) return null;
        return GroupBuyProgressVO.builder()
                .completeCount(groupBuyOrder.getCompleteCount())
                .targetCount(groupBuyOrder.getTargetCount())
                .lockCount(groupBuyOrder.getLockCount())
                .build();
    }

    /**
     * 按活动 ID 查询活动领域实体。
     *
     * @param activityId 活动业务 ID
     * @return 活动领域实体
     */
    @Override
    public GroupBuyActivityEntity queryGroupBuyActivityEntityByActivityId(Long activityId) {
        GroupBuyActivity groupBuyActivity = groupBuyActivityDao.queryGroupBuyActivityByActivityId(activityId);
        return GroupBuyActivityEntity.builder()
                .activityId(groupBuyActivity.getActivityId())
                .activityName(groupBuyActivity.getActivityName())
                .discountId(groupBuyActivity.getDiscountId())
                .groupType(groupBuyActivity.getGroupType())
                .takeLimitCount(groupBuyActivity.getTakeLimitCount())
                .target(groupBuyActivity.getTarget())
                .validTime(groupBuyActivity.getValidTime())
                .status(ActivityStatusEnumVO.valueOf(groupBuyActivity.getStatus()))
                .startTime(groupBuyActivity.getStartTime())
                .endTime(groupBuyActivity.getEndTime())
                .tagId(groupBuyActivity.getTagId())
                .tagScope(groupBuyActivity.getTagScope())
                .build();
    }

    /**
     * 统计某个用户在某活动下的参团次数。
     *
     * @param activityId 活动业务 ID
     * @param userId 用户 ID
     * @return 当前活动参与次数
     */
    @Override
    public Integer queryOrderCountByActivityId(Long activityId, String userId) {
        GroupBuyOrderList groupBuyOrderListReq = new GroupBuyOrderList();
        groupBuyOrderListReq.setActivityId(activityId);
        groupBuyOrderListReq.setUserId(userId);
        return groupBuyOrderListDao.queryOrderCountByActivityId(groupBuyOrderListReq);
    }

    /**
     * 按队伍 ID 查询拼团队伍领域实体。
     * <p>
     * 结果中会顺带构造通知配置对象，供后续成团通知流程直接使用。
     *
     * @param teamId 队伍 ID
     * @return 拼团队伍实体
     */
    @Override
    public GroupBuyTeamEntity queryGroupBuyTeamByTeamId(String teamId) {
        GroupBuyOrder groupBuyOrder = groupBuyOrderDao.queryGroupBuyTeamByTeamId(teamId);
        return GroupBuyTeamEntity.builder()
                .teamId(groupBuyOrder.getTeamId())
                .activityId(groupBuyOrder.getActivityId())
                .targetCount(groupBuyOrder.getTargetCount())
                .completeCount(groupBuyOrder.getCompleteCount())
                .lockCount(groupBuyOrder.getLockCount())
                .status(GroupBuyOrderEnumVO.valueOf(groupBuyOrder.getStatus()))
                .validStartTime(groupBuyOrder.getValidStartTime())
                .validEndTime(groupBuyOrder.getValidEndTime())
                .notifyConfigVO(NotifyConfigVO.builder()
                        .notifyType(NotifyTypeEnumVO.valueOf(groupBuyOrder.getNotifyType()))
                        .notifyUrl(groupBuyOrder.getNotifyUrl())
                        // MQ 是固定的
                        .notifyMQ(topic_team_success)
                        .build())
                .build();
    }

    @Transactional(timeout = 5000)
    /**
     * 处理支付成功后的拼团结算。
     * <p>
     * 该流程分三步：
     * 1. 将用户订单明细从“锁定”更新为“已支付”；
     * 2. 将队伍已完成人数加 1；
     * 3. 如果本次支付正好让队伍达到成团目标，则更新队伍状态并写入通知任务。
     *
     * @param groupBuyTeamSettlementAggregate 结算聚合对象
     * @return 若本次结算触发成团，则返回对应通知任务；否则返回 {@code null}
     */
    @Override
    public NotifyTaskEntity settlementMarketPayOrder(GroupBuyTeamSettlementAggregate groupBuyTeamSettlementAggregate) {

        UserEntity userEntity = groupBuyTeamSettlementAggregate.getUserEntity();
        GroupBuyTeamEntity groupBuyTeamEntity = groupBuyTeamSettlementAggregate.getGroupBuyTeamEntity();
        NotifyConfigVO notifyConfigVO = groupBuyTeamEntity.getNotifyConfigVO();
        TradePaySuccessEntity tradePaySuccessEntity = groupBuyTeamSettlementAggregate.getTradePaySuccessEntity();

        // 1. 更新拼团订单明细状态
        GroupBuyOrderList groupBuyOrderListReq = new GroupBuyOrderList();
        groupBuyOrderListReq.setUserId(userEntity.getUserId());
        groupBuyOrderListReq.setOutTradeNo(tradePaySuccessEntity.getOutTradeNo());
        groupBuyOrderListReq.setOutTradeTime(tradePaySuccessEntity.getOutTradeTime());

        int updateOrderListStatusCount = groupBuyOrderListDao.updateOrderStatus2COMPLETE(groupBuyOrderListReq);
        if (1 != updateOrderListStatusCount) {
            throw new AppException(ResponseCode.UPDATE_ZERO);
        }

        // 2. 更新拼团达成数量
        int updateAddCount = groupBuyOrderDao.updateAddCompleteCount(groupBuyTeamEntity.getTeamId());
        if (1 != updateAddCount) {
            throw new AppException(ResponseCode.UPDATE_ZERO);
        }

        // 3. 更新拼团完成状态
        // 【面试题，这个地方可能会有一个并发情况，就是多个用户拿到的 groupBuyTeamEntity.getCompleteCount() 是同一个值怎么办？】
        // 【方式1；可以给调用 settlementMarketPayOrder 结算方法的地方，添加一个分布式锁，让结算只能顺序执行】
        // 【方式2；这部分结算，只做数据库的更新操作，以及发送mq，之后在消费mq的地方，做结算。】
        // 【方式3；增加一个定时job任务补偿，检索订单量够，但没有结算的拼团组队记录】
        if (groupBuyTeamEntity.getTargetCount() - groupBuyTeamEntity.getCompleteCount() == 1) {
            int updateOrderStatusCount = groupBuyOrderDao.updateOrderStatus2COMPLETE(groupBuyTeamEntity.getTeamId());
            if (1 != updateOrderStatusCount) {
                throw new AppException(ResponseCode.UPDATE_ZERO);
            }

            // 查询拼团交易完成外部单号列表
            List<String> outTradeNoList = groupBuyOrderListDao.queryGroupBuyCompleteOrderOutTradeNoListByTeamId(groupBuyTeamEntity.getTeamId());

            // 拼团完成后不直接在事务里调用外部系统，而是先写本地消息任务表，后续由异步任务统一通知。
            NotifyTask notifyTask = new NotifyTask();
            notifyTask.setActivityId(groupBuyTeamEntity.getActivityId());
            notifyTask.setTeamId(groupBuyTeamEntity.getTeamId());
            notifyTask.setNotifyCategory(TaskNotifyCategoryEnumVO.TRADE_SETTLEMENT.getCode());
            notifyTask.setNotifyType(notifyConfigVO.getNotifyType().getCode());
            notifyTask.setNotifyMQ(NotifyTypeEnumVO.MQ.equals(notifyConfigVO.getNotifyType()) ? notifyConfigVO.getNotifyMQ() : null);
            notifyTask.setNotifyUrl(NotifyTypeEnumVO.HTTP.equals(notifyConfigVO.getNotifyType()) ? notifyConfigVO.getNotifyUrl() : null);
            notifyTask.setNotifyCount(0);
            notifyTask.setNotifyStatus(0);
            notifyTask.setUuid(groupBuyTeamEntity.getTeamId() + Constants.UNDERLINE + TaskNotifyCategoryEnumVO.TRADE_SETTLEMENT.getCode() + Constants.UNDERLINE + tradePaySuccessEntity.getOutTradeNo());

            notifyTask.setParameterJson(JSON.toJSONString(new HashMap<String, Object>() {{
                put("teamId", groupBuyTeamEntity.getTeamId());
                put("outTradeNoList", outTradeNoList);
            }}));

            notifyTaskDao.insert(notifyTask);

            return NotifyTaskEntity.builder()
                    .teamId(notifyTask.getTeamId())
                    .notifyType(notifyTask.getNotifyType())
                    .notifyMQ(notifyTask.getNotifyMQ())
                    .notifyUrl(notifyTask.getNotifyUrl())
                    .notifyCount(notifyTask.getNotifyCount())
                    .parameterJson(notifyTask.getParameterJson())
                    .uuid(notifyTask.getUuid())
                    .build();
        }

        return null;
    }

    /**
     * 判断指定来源渠道是否被黑名单拦截。
     *
     * @param source 渠道标识
     * @param channel 来源标识
     * @return {@code true} 表示被拦截
     */
    @Override
    public boolean isSCBlackIntercept(String source, String channel) {
        return dccService.isSCBlackIntercept(source, channel);
    }

    /**
     * 查询全部待执行或待重试的通知任务。
     *
     * @return 通知任务实体列表
     */
    @Override
    public List<NotifyTaskEntity> queryUnExecutedNotifyTaskList() {
        List<NotifyTask> notifyTaskList = notifyTaskDao.queryUnExecutedNotifyTaskList();
        if (notifyTaskList.isEmpty()) return new ArrayList<>();

        List<NotifyTaskEntity> notifyTaskEntities = new ArrayList<>();
        for (NotifyTask notifyTask : notifyTaskList) {

            NotifyTaskEntity notifyTaskEntity = NotifyTaskEntity.builder()
                    .teamId(notifyTask.getTeamId())
                    .notifyType(notifyTask.getNotifyType())
                    .notifyMQ(notifyTask.getNotifyMQ())
                    .notifyUrl(notifyTask.getNotifyUrl())
                    .notifyCount(notifyTask.getNotifyCount())
                    .parameterJson(notifyTask.getParameterJson())
                    .uuid(notifyTask.getUuid())
                    .build();

            notifyTaskEntities.add(notifyTaskEntity);
        }

        return notifyTaskEntities;
    }

    /**
     * 查询指定队伍的待执行通知任务。
     *
     * @param teamId 队伍 ID
     * @return 通知任务实体列表；未命中时返回空列表
     */
    @Override
    public List<NotifyTaskEntity> queryUnExecutedNotifyTaskList(String teamId) {
        NotifyTask notifyTask = notifyTaskDao.queryUnExecutedNotifyTaskByTeamId(teamId);
        if (null == notifyTask) return new ArrayList<>();
        return Collections.singletonList(NotifyTaskEntity.builder()
                .teamId(notifyTask.getTeamId())
                .notifyType(notifyTask.getNotifyType())
                .notifyMQ(notifyTask.getNotifyMQ())
                .notifyUrl(notifyTask.getNotifyUrl())
                .notifyCount(notifyTask.getNotifyCount())
                .parameterJson(notifyTask.getParameterJson())
                .uuid(notifyTask.getUuid())
                .build());
    }

    /**
     * 将通知任务更新为成功。
     *
     * @param notifyTaskEntity 通知任务实体
     * @return 受影响行数
     */
    @Override
    public int updateNotifyTaskStatusSuccess(NotifyTaskEntity notifyTaskEntity) {
        NotifyTask notifyTask = NotifyTask.builder()
                .teamId(notifyTaskEntity.getTeamId())
                .uuid(notifyTaskEntity.getUuid())
                .build();
        return notifyTaskDao.updateNotifyTaskStatusSuccess(notifyTask);
    }

    /**
     * 将通知任务更新为失败。
     *
     * @param notifyTaskEntity 通知任务实体
     * @return 受影响行数
     */
    @Override
    public int updateNotifyTaskStatusError(NotifyTaskEntity notifyTaskEntity) {
        NotifyTask notifyTask = NotifyTask.builder()
                .teamId(notifyTaskEntity.getTeamId())
                .uuid(notifyTaskEntity.getUuid())
                .build();
        return notifyTaskDao.updateNotifyTaskStatusError(notifyTask);
    }

    /**
     * 将通知任务更新为重试中。
     *
     * @param notifyTaskEntity 通知任务实体
     * @return 受影响行数
     */
    @Override
    public int updateNotifyTaskStatusRetry(NotifyTaskEntity notifyTaskEntity) {
        NotifyTask notifyTask = NotifyTask.builder()
                .teamId(notifyTaskEntity.getTeamId())
                .uuid(notifyTaskEntity.getUuid())
                .build();
        return notifyTaskDao.updateNotifyTaskStatusRetry(notifyTask);
    }

    /**
     * 占用库存
     * <p>
     * 关于 Redis 独占锁和无锁化设计；<a href="https://bugstack.cn/md/road-map/redis.html">Redis 缓存、加锁(独占/分段)、发布/订阅，常用特性的使用和高级编码操作</a>
     */
    @Override
    public boolean occupyTeamStock(String teamStockKey, String recoveryTeamStockKey, Integer target, Integer validTime) {
        // 失败恢复量
        Long recoveryCount = redisService.getAtomicLong(recoveryTeamStockKey);
        recoveryCount = null == recoveryCount ? 0 : recoveryCount;

        // 1. incr 得到当前占用序号，并把 recoveryCount 视为可恢复的“补偿库存”。
        // 2. 从有组队量开始，相当于已经有了一个占用量，所以要 +1。
        long occupy = redisService.incr(teamStockKey) + 1;

        if (occupy > target + recoveryCount) {
            // 超出库存限制时，需要将已经增加的库存减回去，避免库存泄漏
            redisService.decr(teamStockKey);
            return false;
        }

        // 1. 给每个产生的值加锁为兜底设计，虽然incr操作是原子的，基本不会产生一样的值。但在实际生产中，遇到过集群的运维配置问题，以及业务运营配置数据问题，导致incr得到的值相同。
        // 2. validTime + 60分钟，是一个延后时间的设计，让数据保留时间稍微长一些，便于排查问题。
        String lockKey = teamStockKey + Constants.UNDERLINE + occupy;
        Boolean lock = redisService.setNx(lockKey, validTime + 60, TimeUnit.MINUTES);

        if (!lock) {
            log.info("组队库存加锁失败 {}", lockKey);
        }

        return lock;
    }

    /**
     * 增加失败补偿库存。
     * <p>
     * 该方法不直接回滚队伍主库存，而是把恢复量记录到 recovery key 中，
     * 后续新的参团请求会将这部分恢复量一并纳入可用库存计算。
     *
     * @param recoveryTeamStockKey 恢复库存 key
     * @param validTime 队伍有效时长，当前方法中未直接使用，保留作扩展参数
     */
    @Override
    public void recoveryTeamStock(String recoveryTeamStockKey, Integer validTime) {
        // 首次组队拼团，是没有 teamId 的，所以不需要这个做处理。
        if (StringUtils.isBlank(recoveryTeamStockKey)) return;

        redisService.incr(recoveryTeamStockKey);
    }

    @Transactional(timeout = 5000)
    /**
     * 处理“未支付即退单”场景。
     * <p>
     * 该场景下用户明细从锁定态转为退单态，队伍锁单人数同步回滚，
     * 最后写入一条退款通知任务，交给异步任务继续对外通知。
     *
     * @param groupBuyRefundAggregate 退款聚合对象
     * @return 生成的通知任务实体
     */
    @Override
    public NotifyTaskEntity unpaid2Refund(GroupBuyRefundAggregate groupBuyRefundAggregate) {
        TradeRefundOrderEntity tradeRefundOrderEntity = groupBuyRefundAggregate.getTradeRefundOrderEntity();
        GroupBuyProgressVO groupBuyProgress = groupBuyRefundAggregate.getGroupBuyProgress();

        GroupBuyOrderList groupBuyOrderListReq = new GroupBuyOrderList();
        // 保留userId，企业中往往会根据 userId 作为分库分表路由键，如果将来做分库分表也可以方便处理
        groupBuyOrderListReq.setUserId(tradeRefundOrderEntity.getUserId());
        groupBuyOrderListReq.setOrderId(tradeRefundOrderEntity.getOrderId());

        int updateUnpaid2RefundCount = groupBuyOrderListDao.unpaid2Refund(groupBuyOrderListReq);
        if (1 != updateUnpaid2RefundCount) {
            log.error("逆向流程-unpaid2Refund，更新订单状态(退单)失败 {} {}", tradeRefundOrderEntity.getUserId(), tradeRefundOrderEntity.getOrderId());
            throw new AppException(ResponseCode.UPDATE_ZERO);
        }

        GroupBuyOrder groupBuyOrderReq = new GroupBuyOrder();
        groupBuyOrderReq.setTeamId(tradeRefundOrderEntity.getTeamId());
        groupBuyOrderReq.setLockCount(groupBuyProgress.getLockCount());

        int updateTeamUnpaid2Refund = groupBuyOrderDao.unpaid2Refund(groupBuyOrderReq);
        if (1 != updateTeamUnpaid2Refund) {
            log.error("逆向流程-unpaid2Refund，更新组队记录(退单)失败 {} {}", tradeRefundOrderEntity.getUserId(), tradeRefundOrderEntity.getOrderId());
            throw new AppException(ResponseCode.UPDATE_ZERO);
        }

        // 把退款事实封装成 MQ 通知任务，由异步链路继续做最终一致性处理。
        NotifyTask notifyTask = new NotifyTask();
        notifyTask.setActivityId(tradeRefundOrderEntity.getActivityId());
        notifyTask.setTeamId(tradeRefundOrderEntity.getTeamId());
        notifyTask.setNotifyCategory(TaskNotifyCategoryEnumVO.TRADE_UNPAID2REFUND.getCode());
        notifyTask.setNotifyType(NotifyTypeEnumVO.MQ.getCode());
        notifyTask.setNotifyMQ(topic_team_refund);
        notifyTask.setNotifyCount(0);
        notifyTask.setNotifyStatus(0);
        notifyTask.setUuid(tradeRefundOrderEntity.getTeamId() + Constants.UNDERLINE + TaskNotifyCategoryEnumVO.TRADE_UNPAID2REFUND.getCode() + Constants.UNDERLINE + tradeRefundOrderEntity.getOrderId());

        notifyTask.setParameterJson(JSON.toJSONString(new HashMap<String, Object>() {{
            put("type", RefundTypeEnumVO.UNPAID_UNLOCK.getCode());
            put("userId", tradeRefundOrderEntity.getUserId());
            put("teamId", tradeRefundOrderEntity.getTeamId());
            put("orderId", tradeRefundOrderEntity.getOrderId());
            put("outTradeNo", tradeRefundOrderEntity.getOutTradeNo());
            put("activityId", tradeRefundOrderEntity.getActivityId());
        }}));

        notifyTaskDao.insert(notifyTask);

        return NotifyTaskEntity.builder()
                .teamId(notifyTask.getTeamId())
                .notifyType(notifyTask.getNotifyType())
                .notifyMQ(notifyTask.getNotifyMQ())
                .notifyCount(notifyTask.getNotifyCount())
                .parameterJson(notifyTask.getParameterJson())
                .uuid(notifyTask.getUuid())
                .build();
    }

    @Transactional(timeout = 5000)
    /**
     * 处理“已支付但队伍未成团”的退单场景。
     *
     * @param groupBuyRefundAggregate 退款聚合对象
     * @return 生成的通知任务实体
     */
    @Override
    public NotifyTaskEntity paid2Refund(GroupBuyRefundAggregate groupBuyRefundAggregate) {
        TradeRefundOrderEntity tradeRefundOrderEntity = groupBuyRefundAggregate.getTradeRefundOrderEntity();
        GroupBuyProgressVO groupBuyProgress = groupBuyRefundAggregate.getGroupBuyProgress();

        GroupBuyOrderList groupBuyOrderListReq = new GroupBuyOrderList();
        // 保留userId，企业中往往会根据 userId 作为分库分表路由键，如果将来做分库分表也可以方便处理
        groupBuyOrderListReq.setUserId(tradeRefundOrderEntity.getUserId());
        groupBuyOrderListReq.setOrderId(tradeRefundOrderEntity.getOrderId());

        int updatePaid2RefundCount = groupBuyOrderListDao.paid2Refund(groupBuyOrderListReq);
        if (1 != updatePaid2RefundCount) {
            log.error("逆向流程-paid2Refund，更新订单状态(退单)失败 {} {}", tradeRefundOrderEntity.getUserId(), tradeRefundOrderEntity.getOrderId());
            throw new AppException(ResponseCode.UPDATE_ZERO);
        }

        GroupBuyOrder groupBuyOrderReq = new GroupBuyOrder();
        groupBuyOrderReq.setTeamId(tradeRefundOrderEntity.getTeamId());
        groupBuyOrderReq.setLockCount(groupBuyProgress.getLockCount());
        groupBuyOrderReq.setCompleteCount(groupBuyProgress.getCompleteCount());

        int updateTeamPaid2Refund = groupBuyOrderDao.paid2Refund(groupBuyOrderReq);
        if (1 != updateTeamPaid2Refund) {
            log.error("逆向流程-paid2Refund，更新组队记录(退单)失败 {} {}", tradeRefundOrderEntity.getUserId(), tradeRefundOrderEntity.getOrderId());
            throw new AppException(ResponseCode.UPDATE_ZERO);
        }

        // 仍然通过本地消息表把退款结果异步发给下游，而不是在事务中直接远程调用。
        NotifyTask notifyTask = new NotifyTask();
        notifyTask.setActivityId(tradeRefundOrderEntity.getActivityId());
        notifyTask.setTeamId(tradeRefundOrderEntity.getTeamId());
        notifyTask.setNotifyCategory(TaskNotifyCategoryEnumVO.TRADE_PAID2REFUND.getCode());
        notifyTask.setNotifyType(NotifyTypeEnumVO.MQ.getCode());
        notifyTask.setNotifyMQ(topic_team_refund);
        notifyTask.setNotifyCount(0);
        notifyTask.setNotifyStatus(0);
        notifyTask.setUuid(tradeRefundOrderEntity.getTeamId() + Constants.UNDERLINE + TaskNotifyCategoryEnumVO.TRADE_PAID2REFUND.getCode() + Constants.UNDERLINE + tradeRefundOrderEntity.getOrderId());

        notifyTask.setParameterJson(JSON.toJSONString(new HashMap<String, Object>() {{
            put("type", RefundTypeEnumVO.PAID_UNFORMED.getCode());
            put("userId", tradeRefundOrderEntity.getUserId());
            put("teamId", tradeRefundOrderEntity.getTeamId());
            put("orderId", tradeRefundOrderEntity.getOrderId());
            put("outTradeNo", tradeRefundOrderEntity.getOutTradeNo());
            put("activityId", tradeRefundOrderEntity.getActivityId());
        }}));

        notifyTaskDao.insert(notifyTask);

        return NotifyTaskEntity.builder()
                .teamId(notifyTask.getTeamId())
                .notifyType(notifyTask.getNotifyType())
                .notifyMQ(notifyTask.getNotifyMQ())
                .notifyCount(notifyTask.getNotifyCount())
                .parameterJson(notifyTask.getParameterJson())
                .uuid(notifyTask.getUuid())
                .build();
    }

    @Transactional(timeout = 5000)
    /**
     * 处理“已成团后成员退单”的逆向流程。
     * <p>
     * 该方法会根据退款后队伍的剩余完成状态，决定将队伍更新为：
     * 仍然完成但含退单，或彻底失败。
     *
     * @param groupBuyRefundAggregate 退款聚合对象
     * @return 生成的通知任务实体
     */
    @Override
    public NotifyTaskEntity paidTeam2Refund(GroupBuyRefundAggregate groupBuyRefundAggregate) {
        TradeRefundOrderEntity tradeRefundOrderEntity = groupBuyRefundAggregate.getTradeRefundOrderEntity();
        GroupBuyProgressVO groupBuyProgress = groupBuyRefundAggregate.getGroupBuyProgress();
        GroupBuyOrderEnumVO groupBuyOrderEnumVO = groupBuyRefundAggregate.getGroupBuyOrderEnumVO();

        GroupBuyOrderList groupBuyOrderListReq = new GroupBuyOrderList();
        // 保留userId，企业中往往会根据 userId 作为分库分表路由键，如果将来做分库分表也可以方便处理
        groupBuyOrderListReq.setUserId(tradeRefundOrderEntity.getUserId());
        groupBuyOrderListReq.setOrderId(tradeRefundOrderEntity.getOrderId());

        int updatePaid2RefundCount = groupBuyOrderListDao.paidTeam2Refund(groupBuyOrderListReq);
        if (1 != updatePaid2RefundCount) {
            log.error("逆向流程-paidTeam2Refund，更新订单状态(退单)失败 {} {}", tradeRefundOrderEntity.getUserId(), tradeRefundOrderEntity.getOrderId());
            throw new AppException(ResponseCode.UPDATE_ZERO);
        }

        GroupBuyOrder groupBuyOrderReq = new GroupBuyOrder();
        groupBuyOrderReq.setTeamId(tradeRefundOrderEntity.getTeamId());
        groupBuyOrderReq.setLockCount(groupBuyProgress.getLockCount());
        groupBuyOrderReq.setCompleteCount(groupBuyProgress.getCompleteCount());

        // 根据退款后剩余人数，决定是“成团但有退单”还是“整个团失败”。
        if (GroupBuyOrderEnumVO.COMPLETE_FAIL.equals(groupBuyOrderEnumVO)) {
            int updateTeamPaid2Refund = groupBuyOrderDao.paidTeam2Refund(groupBuyOrderReq);
            if (1 != updateTeamPaid2Refund) {
                log.error("逆向流程-paidTeam2Refund，更新组队记录(退单)失败 {} {}", tradeRefundOrderEntity.getUserId(), tradeRefundOrderEntity.getOrderId());
                throw new AppException(ResponseCode.UPDATE_ZERO);
            }
        } else if (GroupBuyOrderEnumVO.FAIL.equals(groupBuyOrderEnumVO)){
            int updateTeamPaid2RefundFail = groupBuyOrderDao.paidTeam2RefundFail(groupBuyOrderReq);
            if (1 != updateTeamPaid2RefundFail) {
                log.error("逆向流程-updateTeamPaid2RefundFail，更新组队记录(退单)失败 {} {}", tradeRefundOrderEntity.getUserId(), tradeRefundOrderEntity.getOrderId());
                throw new AppException(ResponseCode.UPDATE_ZERO);
            }
        }

        // 已成团后退单的通知语义与前两种不同，因此使用单独的通知类别编码。
        NotifyTask notifyTask = new NotifyTask();
        notifyTask.setActivityId(tradeRefundOrderEntity.getActivityId());
        notifyTask.setTeamId(tradeRefundOrderEntity.getTeamId());
        notifyTask.setNotifyCategory(TaskNotifyCategoryEnumVO.TRADE_PAID_TEAM2REFUND.getCode());
        notifyTask.setNotifyType(NotifyTypeEnumVO.MQ.getCode());
        notifyTask.setNotifyMQ(topic_team_refund);
        notifyTask.setNotifyCount(0);
        notifyTask.setNotifyStatus(0);
        notifyTask.setUuid(tradeRefundOrderEntity.getTeamId() + Constants.UNDERLINE + TaskNotifyCategoryEnumVO.TRADE_PAID_TEAM2REFUND.getCode() + Constants.UNDERLINE + tradeRefundOrderEntity.getOrderId());

        notifyTask.setParameterJson(JSON.toJSONString(new HashMap<String, Object>() {{
            put("type", RefundTypeEnumVO.PAID_FORMED.getCode());
            put("userId", tradeRefundOrderEntity.getUserId());
            put("teamId", tradeRefundOrderEntity.getTeamId());
            put("orderId", tradeRefundOrderEntity.getOrderId());
            put("outTradeNo", tradeRefundOrderEntity.getOutTradeNo());
            put("activityId", tradeRefundOrderEntity.getActivityId());
        }}));

        notifyTaskDao.insert(notifyTask);

        return NotifyTaskEntity.builder()
                .teamId(notifyTask.getTeamId())
                .notifyType(notifyTask.getNotifyType())
                .notifyMQ(notifyTask.getNotifyMQ())
                .notifyCount(notifyTask.getNotifyCount())
                .parameterJson(notifyTask.getParameterJson())
                .build();
    }

    /**
     * 在退款后补记恢复库存。
     * <p>
     * 为防止同一订单因为 MQ 重复消费或重试而多次恢复库存，
     * 这里会先基于 orderId 做一次幂等锁保护。
     *
     * @param recoveryTeamStockKey 恢复库存 key
     * @param orderId 订单 ID，用作幂等锁 key 的核心部分
     */
    @Override
    public void refund2AddRecovery(String recoveryTeamStockKey, String orderId) {
        // 如果恢复库存key为空，直接返回
        if (StringUtils.isBlank(recoveryTeamStockKey) || StringUtils.isBlank(orderId)) {
            return;
        }

        // 使用orderId作为锁的key，避免同一订单重复恢复库存
        String lockKey = "refund_lock_" + orderId;
        
        // 尝试获取分布式锁，防止重复操作。这里通过长过期时间覆盖 MQ 重试窗口。
        Boolean lockAcquired = redisService.setNx(lockKey, 30 * 24 * 60 * 60 * 1000L, TimeUnit.MINUTES);
        
        if (!lockAcquired) {
            log.warn("订单 {} 恢复库存操作已在进行中，跳过重复操作", orderId);
            return;
        }

        try {
            // 在锁保护下执行库存恢复操作
            redisService.incr(recoveryTeamStockKey);
            log.info("订单 {} 恢复库存成功，恢复库存key: {}", orderId, recoveryTeamStockKey);
        } catch (Exception e) {
            log.error("订单 {} 恢复库存失败，恢复库存key: {}", orderId, recoveryTeamStockKey, e);
            // 如果抛异常则释放锁，允许MQ重新消费恢复库存
            redisService.remove(lockKey);
            throw e;
        }

    }

    /**
     * 查询超时未支付的用户拼团明细，并补齐对应队伍信息。
     * <p>
     * 该方法主要服务于超时退单定时任务，让任务侧无需自己再分别查订单与队伍。
     *
     * @return 超时未支付的用户拼团详情列表
     */
    @Override
    public List<UserGroupBuyOrderDetailEntity> queryTimeoutUnpaidOrderList() {
        List<GroupBuyOrderList> groupBuyOrderLists = groupBuyOrderListDao.queryTimeoutUnpaidOrderList();
        if (null == groupBuyOrderLists || groupBuyOrderLists.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 获取所有teamId
        Set<String> teamIds = groupBuyOrderLists.stream()
                .map(GroupBuyOrderList::getTeamId)
                .collect(Collectors.toSet());
        
        // 查询团队信息
        List<GroupBuyOrder> groupBuyOrders = groupBuyOrderDao.queryGroupBuyTeamByTeamIds(teamIds);
        if (null == groupBuyOrders || groupBuyOrders.isEmpty()) {
            return new ArrayList<>();
        }
        
        Map<String, GroupBuyOrder> groupBuyOrderMap = groupBuyOrders.stream()
                .collect(Collectors.toMap(GroupBuyOrder::getTeamId, order -> order));
        
        // 把“超时订单 + 队伍主单”拼装成任务侧可直接消费的统一视图对象。
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
                    .source(groupBuyOrderList.getSource())
                    .channel(groupBuyOrderList.getChannel())
                    .build();
            
            userGroupBuyOrderDetailEntities.add(userGroupBuyOrderDetailEntity);
        }
        
        return userGroupBuyOrderDetailEntities;
    }

}
