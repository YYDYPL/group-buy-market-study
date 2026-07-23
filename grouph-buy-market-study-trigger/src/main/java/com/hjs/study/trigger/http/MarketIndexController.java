package com.hjs.study.trigger.http;

import com.hjs.study.api.IMarketIndexService;
import com.hjs.study.api.dto.GoodsMarketRequestDTO;
import com.hjs.study.api.dto.GoodsMarketResponseDTO;
import com.hjs.study.api.response.Response;
import com.hjs.study.domain.activity.model.entity.MarketProductEntity;
import com.hjs.study.domain.activity.model.entity.TrialBalanceEntity;
import com.hjs.study.domain.activity.model.entity.UserGroupBuyOrderDetailEntity;
import com.hjs.study.domain.activity.model.valobj.GroupBuyActivityDiscountVO;
import com.hjs.study.domain.activity.model.valobj.TeamStatisticVO;
import com.hjs.study.domain.activity.service.IIndexGroupBuyMarketService;
import com.hjs.study.types.enums.ResponseCode;
import cn.bugstack.wrench.rate.limiter.types.annotations.RateLimiterAccessInterceptor;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 拼团营销首页的 HTTP 适配器。
 *
 * <p>面向商品详情页聚合一次营销展示所需的数据：商品优惠试算结果、当前可参与的拼团队伍，
 * 以及活动维度的拼团统计。控制器负责协议转换和响应组装，活动匹配、优惠计算、人群判断等
 * 业务规则均委托给 {@link IIndexGroupBuyMarketService}。</p>
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @create 2025-02-02 16:03
 */
@Slf4j
@RestController()
@CrossOrigin("*")
@RequestMapping("/api/v1/gbm/index/")
public class MarketIndexController implements IMarketIndexService {

    /** 拼团首页领域服务，提供营销试算、可参与队伍查询和活动统计能力。 */
    @Resource
    private IIndexGroupBuyMarketService indexGroupBuyMarketService;

    /**
     * 查询指定商品在当前来源、渠道和用户上下文中的拼团营销配置。
     *
     * <p>接口按用户 ID 限流，每秒允许 1 次访问；达到限流条件后由
     * {@link #queryGroupBuyMarketConfigFallBack(GoodsMarketRequestDTO)} 返回统一限流响应。</p>
     *
     * <p>处理流程：</p>
     * <ol>
     *     <li>校验用户、来源、渠道和商品标识；</li>
     *     <li>执行营销试算，确定命中的活动并计算优惠价格；</li>
     *     <li>查询可参与的拼团队伍及活动统计；</li>
     *     <li>把领域实体转换为面向前端的商品、队伍和统计 DTO。</li>
     * </ol>
     *
     * @param requestDTO 营销查询条件，包含用户、来源、渠道和商品标识
     * @return 成功时返回页面展示所需的聚合数据；参数非法或处理异常时返回对应错误码
     */
    @RateLimiterAccessInterceptor(key = "userId", fallbackMethod = "queryGroupBuyMarketConfigFallBack", permitsPerSecond = 1.0d, blacklistCount = 1)
    @RequestMapping(value = "query_group_buy_market_config", method = RequestMethod.POST)
    @Override
    public Response<GoodsMarketResponseDTO> queryGroupBuyMarketConfig(@RequestBody GoodsMarketRequestDTO requestDTO) {
        try {
            log.info("查询拼团营销配置开始:{} goodsId:{}", requestDTO.getUserId(), requestDTO.getGoodsId());

            // 这些字段共同决定活动匹配范围，任一缺失都无法得到确定的营销结果。
            if (StringUtils.isBlank(requestDTO.getUserId()) || StringUtils.isBlank(requestDTO.getSource()) || StringUtils.isBlank(requestDTO.getChannel()) || StringUtils.isBlank(requestDTO.getGoodsId())) {
                return Response.<GoodsMarketResponseDTO>builder()
                        .code(ResponseCode.ILLEGAL_PARAMETER.getCode())
                        .info(ResponseCode.ILLEGAL_PARAMETER.getInfo())
                        .build();
            }

            // 1. 将接口参数转换为领域入参，试算会完成活动匹配、商品查询和优惠金额计算。
            TrialBalanceEntity trialBalanceEntity = indexGroupBuyMarketService.indexMarketTrial(MarketProductEntity.builder()
                    .userId(requestDTO.getUserId())
                    .source(requestDTO.getSource())
                    .channel(requestDTO.getChannel())
                    .goodsId(requestDTO.getGoodsId())
                    .build());


            // 后续队伍和统计查询均以本次试算实际命中的活动 ID 为准。
            GroupBuyActivityDiscountVO groupBuyActivityDiscountVO = trialBalanceEntity.getGroupBuyActivityDiscountVO();
            Long activityId = groupBuyActivityDiscountVO.getActivityId();

            // 2. 查询用户可参与的进行中队伍；第 1 页、每页 2 条用于首页精简展示。
            List<UserGroupBuyOrderDetailEntity> userGroupBuyOrderDetailEntities = indexGroupBuyMarketService.queryInProgressUserGroupBuyOrderDetailList(activityId, requestDTO.getUserId(), 1, 2);

            // 3. 查询活动全局统计，与上一步的具体队伍列表属于不同展示维度。
            TeamStatisticVO teamStatisticVO = indexGroupBuyMarketService.queryTeamStatisticByActivityId(activityId);

            // 将领域试算结果映射为 API 商品视图，避免领域对象直接暴露给调用方。
            GoodsMarketResponseDTO.Goods goods = GoodsMarketResponseDTO.Goods.builder()
                    .goodsId(trialBalanceEntity.getGoodsId())
                    .originalPrice(trialBalanceEntity.getOriginalPrice())
                    .deductionPrice(trialBalanceEntity.getDeductionPrice())
                    .payPrice(trialBalanceEntity.getPayPrice())
                    .build();

            // 始终返回空集合而不是 null，调用方可以直接遍历 teamList。
            List<GoodsMarketResponseDTO.Team> teams = new ArrayList<>();
            if (null != userGroupBuyOrderDetailEntities && !userGroupBuyOrderDetailEntities.isEmpty()) {
                for (UserGroupBuyOrderDetailEntity userGroupBuyOrderDetailEntity : userGroupBuyOrderDetailEntities) {
                    GoodsMarketResponseDTO.Team team = GoodsMarketResponseDTO.Team.builder()
                            .userId(userGroupBuyOrderDetailEntity.getUserId())
                            .teamId(userGroupBuyOrderDetailEntity.getTeamId())
                            .activityId(userGroupBuyOrderDetailEntity.getActivityId())
                            .targetCount(userGroupBuyOrderDetailEntity.getTargetCount())
                            .completeCount(userGroupBuyOrderDetailEntity.getCompleteCount())
                            .lockCount(userGroupBuyOrderDetailEntity.getLockCount())
                            .validStartTime(userGroupBuyOrderDetailEntity.getValidStartTime())
                            .validEndTime(userGroupBuyOrderDetailEntity.getValidEndTime())
                            // 倒计时在响应组装时动态计算，反映接口被调用这一刻的剩余有效时间。
                            .validTimeCountdown(GoodsMarketResponseDTO.Team.differenceDateTime2Str(new Date(), userGroupBuyOrderDetailEntity.getValidEndTime()))
                            .outTradeNo(userGroupBuyOrderDetailEntity.getOutTradeNo())
                            .build();
                    teams.add(team);
                }
            }

            // 汇总活动累计开团数、成团数和参与人数，供首页展示活动热度。
            GoodsMarketResponseDTO.TeamStatistic teamStatistic = GoodsMarketResponseDTO.TeamStatistic.builder()
                    .allTeamCount(teamStatisticVO.getAllTeamCount())
                    .allTeamCompleteCount(teamStatisticVO.getAllTeamCompleteCount())
                    .allTeamUserCount(teamStatisticVO.getAllTeamUserCount())
                    .build();

            Response<GoodsMarketResponseDTO> response = Response.<GoodsMarketResponseDTO>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(GoodsMarketResponseDTO.builder()
                            .activityId(activityId)
                            .goods(goods)
                            .teamList(teams)
                            .teamStatistic(teamStatistic)
                            .build())
                    .build();

            log.info("查询拼团营销配置完成:{} goodsId:{} response:{}", requestDTO.getUserId(), requestDTO.getGoodsId(), JSON.toJSONString(response));

            return response;
        } catch (Exception e) {
            log.error("查询拼团营销配置失败:{} goodsId:{}", requestDTO.getUserId(), requestDTO.getGoodsId(), e);
            return Response.<GoodsMarketResponseDTO>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

    /**
     * 营销配置查询的限流降级方法。
     *
     * <p>方法签名需要与被拦截方法保持兼容，供限流切面通过反射调用。该分支不再访问领域服务，
     * 避免流量已经过载时继续消耗数据库、缓存等下游资源。</p>
     *
     * @param requestDTO 触发限流的原始请求
     * @return 带有限流错误码的统一响应
     */
    public Response<GoodsMarketResponseDTO> queryGroupBuyMarketConfigFallBack(@RequestBody GoodsMarketRequestDTO requestDTO) {
        log.error("查询拼团营销配置限流:{}", requestDTO.getUserId());
        return Response.<GoodsMarketResponseDTO>builder()
                .code(ResponseCode.RATE_LIMITER.getCode())
                .info(ResponseCode.RATE_LIMITER.getInfo())
                .build();
    }

}
