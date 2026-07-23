package com.hjs.study.domain.activity.service;

import com.hjs.study.domain.activity.adapter.repository.IActivityRepository;
import com.hjs.study.domain.activity.model.entity.MarketProductEntity;
import com.hjs.study.domain.activity.model.entity.TrialBalanceEntity;
import com.hjs.study.domain.activity.model.entity.UserGroupBuyOrderDetailEntity;
import com.hjs.study.domain.activity.model.valobj.TeamStatisticVO;
import com.hjs.study.domain.activity.service.trial.factory.DefaultActivityStrategyFactory;
import com.hjs.study.types.design.framework.tree.StrategyHandler;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 首页拼团营销服务实现。
 * <p>
 * 该类是活动域对外暴露的 Facade 入口之一，
 * 向上承接 controller / trigger 层请求，
 * 向下编排策略树试算和仓储查询。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 首页营销服务
 * @create 2024-12-14 14:33
 */
@Service
public class IndexGroupBuyMarketServiceImpl implements IIndexGroupBuyMarketService {

    /** 默认活动策略工厂，负责返回试算流程树的根处理器。 */
    @Resource
    private DefaultActivityStrategyFactory defaultActivityStrategyFactory;
    /** 活动域仓储，负责查询进行中拼团列表和统计数据。 */
    @Resource
    private IActivityRepository repository;

    @Override
    /**
     * 执行首页商品拼团试算。
     * <p>
     * 本方法本身不直接写规则，而是把请求转交给策略树入口，
     * 由各节点依次完成参数校验、切量、营销计算、标签判断和结果组装。
     *
     * @param marketProductEntity 试算输入参数
     * @return 试算结果
     * @throws Exception 试算过程中的异常
     */
    public TrialBalanceEntity indexMarketTrial(MarketProductEntity marketProductEntity) throws Exception {
        // 获取执行策略
        StrategyHandler<MarketProductEntity, DefaultActivityStrategyFactory.DynamicContext, TrialBalanceEntity> strategyHandler = defaultActivityStrategyFactory.strategyHandler();
        // 受理试算操作
        return strategyHandler.apply(marketProductEntity, new DefaultActivityStrategyFactory.DynamicContext());
    }

    @Override
    /**
     * 查询进行中的拼团明细列表。
     * <p>
     * 结果由两部分拼接而成：
     * 1. 当前用户自己参与的拼团；
     * 2. 其他用户正在进行中的拼团随机样本。
     *
     * @param activityId 活动 ID
     * @param userId 用户 ID
     * @param ownerCount 个人列表返回条数
     * @param randomCount 随机列表返回条数
     * @return 合并后的拼团明细列表
     */
    public List<UserGroupBuyOrderDetailEntity> queryInProgressUserGroupBuyOrderDetailList(Long activityId, String userId, Integer ownerCount, Integer randomCount) {
        List<UserGroupBuyOrderDetailEntity> unionAllList = new ArrayList<>();

        // 查询个人拼团数据
        if (0 != ownerCount) {
            List<UserGroupBuyOrderDetailEntity> ownerList = repository.queryInProgressUserGroupBuyOrderDetailListByOwner(activityId, userId, ownerCount);
            if (null != ownerList && !ownerList.isEmpty()){
                unionAllList.addAll(ownerList);
            }
        }

        // 查询其他用户的拼团记录，用于营造“大家都在拼”的展示效果。
        if (0 != randomCount) {
            List<UserGroupBuyOrderDetailEntity> randomList = repository.queryInProgressUserGroupBuyOrderDetailListByRandom(activityId, userId, randomCount);
            if (null != randomList && !randomList.isEmpty()){
                unionAllList.addAll(randomList);
            }
        }

        return unionAllList;
    }

    @Override
    /**
     * 查询活动维度的拼团队伍统计。
     *
     * @param activityId 活动 ID
     * @return 队伍统计结果
     */
    public TeamStatisticVO queryTeamStatisticByActivityId(Long activityId) {
        return repository.queryTeamStatisticByActivityId(activityId);
    }

}
