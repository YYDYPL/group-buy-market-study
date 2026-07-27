package com.hjs.study.trigger.http;

import com.hjs.study.api.IMarketTradeService;
import com.hjs.study.api.dto.*;
import com.hjs.study.api.response.Response;
import com.hjs.study.domain.activity.model.entity.MarketProductEntity;
import com.hjs.study.domain.activity.model.entity.TrialBalanceEntity;
import com.hjs.study.domain.activity.model.valobj.GroupBuyActivityDiscountVO;
import com.hjs.study.domain.activity.service.IIndexGroupBuyMarketService;
import com.hjs.study.domain.trade.model.entity.*;
import com.hjs.study.domain.trade.model.valobj.NotifyConfigVO;
import com.hjs.study.domain.trade.model.valobj.NotifyTypeEnumVO;
import com.hjs.study.domain.trade.model.valobj.TradeOrderStatusEnumVO;
import com.hjs.study.domain.trade.service.ITradeLockOrderService;
import com.hjs.study.domain.trade.service.ITradeRefundOrderService;
import com.hjs.study.domain.trade.service.ITradeSettlementOrderService;
import com.hjs.study.types.enums.ResponseCode;
import com.hjs.study.types.enums.GroupBuyOrderEnumVO;
import com.hjs.study.types.exception.AppException;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 拼团营销交易的 HTTP 适配器。
 *
 * <p>对外提供锁单、支付结算和退单三个交易入口。该类负责请求校验、API DTO 与领域对象之间
 * 的转换，以及把领域异常转换为统一响应；库存占用、成团判断、订单状态流转等核心规则由
 * 对应的交易领域服务完成。</p>
 *
 * <p>典型调用顺序为：先锁单生成营销订单，外部支付成功后调用结算；用户取消或订单超时时
 * 调用退单。三类接口使用 {@code userId + outTradeNo} 关联同一笔外部交易。</p>
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @create 2025-01-11 14:01
 */
@Slf4j
@RestController()
@CrossOrigin("*")
@RequestMapping("/api/v1/gbm/trade/")
public class MarketTradeController implements IMarketTradeService {

    /** 营销首页领域服务，用于锁单前重新试算活动资格与成交价格。 */
    @Resource
    private IIndexGroupBuyMarketService indexGroupBuyMarketService;
    /** 交易锁单领域服务，负责订单幂等检查、拼团库存占用和锁单落库。 */
    @Resource
    private ITradeLockOrderService tradeOrderService;
    /** 交易结算领域服务，负责支付成功后的订单状态流转及成团处理。 */
    @Resource
    private ITradeSettlementOrderService tradeSettlementOrderService;
    /** 交易退单领域服务，负责按当前订单状态选择退单策略并恢复相关资源。 */
    @Resource
    private ITradeRefundOrderService tradeRefundOrderService;

    /**
     * 为一笔拼团交易创建或复用待支付营销订单。
     *
     * <p>锁单会先按外部交易号执行幂等检查，再校验目标队伍是否仍有位置；校验通过后重新进行
     * 营销试算，确保使用服务端当前生效的活动资格和价格，而不是信任客户端提交的金额。</p>
     *
     * <p>{@code teamId} 为空表示发起新队伍，非空表示加入已有队伍。HTTP 回调方式还要求提供
     * 回调地址，通知配置会随订单保存，供拼团完成后的通知任务使用。</p>
     *
     * @param requestDTO 锁单请求，包含用户、商品、活动、外部交易号、可选队伍及通知配置
     * @return 锁单成功时返回内部订单号、成交价格、订单状态和队伍 ID
     */
    @RequestMapping(value = "lock_market_pay_order", method = RequestMethod.POST)
    @Override
    public Response<LockMarketPayOrderResponseDTO> lockMarketPayOrder(@RequestBody LockMarketPayOrderRequestDTO requestDTO) {
        try {
            // 提取请求字段，后续校验和领域对象组装统一使用这些局部变量。
            String userId = requestDTO.getUserId();
            String source = requestDTO.getSource();
            String channel = requestDTO.getChannel();
            String goodsId = requestDTO.getGoodsId();
            Long activityId = requestDTO.getActivityId();
            String outTradeNo = requestDTO.getOutTradeNo();
            String teamId = requestDTO.getTeamId();
            LockMarketPayOrderRequestDTO.NotifyConfigVO notifyConfigVO = requestDTO.getNotifyConfigVO();

            log.info("营销交易锁单:{} LockMarketPayOrderRequestDTO:{}", userId, JSON.toJSONString(requestDTO));

            // 基础业务上下文必须完整；选择 HTTP 通知时，回调地址也是必填项。
            boolean invalidNotify = null == notifyConfigVO
                    || StringUtils.isBlank(notifyConfigVO.getNotifyType())
                    || (!"MQ".equals(notifyConfigVO.getNotifyType()) && !"HTTP".equals(notifyConfigVO.getNotifyType()))
                    || ("HTTP".equals(notifyConfigVO.getNotifyType()) && StringUtils.isBlank(notifyConfigVO.getNotifyUrl()));
            if (StringUtils.isBlank(userId) || StringUtils.isBlank(source)
                    || StringUtils.isBlank(channel) || StringUtils.isBlank(goodsId)
                    || null == activityId || StringUtils.isBlank(outTradeNo) || invalidNotify) {
                return Response.<LockMarketPayOrderResponseDTO>builder()
                        .code(ResponseCode.ILLEGAL_PARAMETER.getCode())
                        .info(ResponseCode.ILLEGAL_PARAMETER.getInfo())
                        .build();
            }

            // 幂等处理：同一用户、同一外部交易号已有待支付订单时，直接返回原锁单结果。
            MarketPayOrderEntity marketPayOrderEntity = tradeOrderService.queryNoPayMarketPayOrderByOutTradeNo(userId, outTradeNo);
            if (null != marketPayOrderEntity && TradeOrderStatusEnumVO.CREATE.equals(marketPayOrderEntity.getTradeOrderStatusEnumVO())) {
                LockMarketPayOrderResponseDTO lockMarketPayOrderResponseDTO = LockMarketPayOrderResponseDTO.builder()
                        .orderId(marketPayOrderEntity.getOrderId())
                        .originalPrice(marketPayOrderEntity.getOriginalPrice())
                        .deductionPrice(marketPayOrderEntity.getDeductionPrice())
                        .payPrice(marketPayOrderEntity.getPayPrice())
                        .tradeOrderStatus(marketPayOrderEntity.getTradeOrderStatusEnumVO().getCode())
                        .teamId(marketPayOrderEntity.getTeamId())
                        .build();

                log.info("交易锁单记录(存在):{} marketPayOrderEntity:{}", userId, JSON.toJSONString(marketPayOrderEntity));
                return Response.<LockMarketPayOrderResponseDTO>builder()
                        .code(ResponseCode.SUCCESS.getCode())
                        .info(ResponseCode.SUCCESS.getInfo())
                        .data(lockMarketPayOrderResponseDTO)
                        .build();
            }

            // 加入已有队伍前先做快速容量校验，避免已满队伍继续进入后续试算和锁单流程。
            if (StringUtils.isNotBlank(teamId)) {
                GroupBuyTeamEntity team = tradeOrderService.queryGroupBuyTeamByTeamId(teamId);
                boolean unavailable = null == team
                        || !Objects.equals(activityId, team.getActivityId())
                        || !GroupBuyOrderEnumVO.PROGRESS.equals(team.getStatus())
                        || null == team.getValidEndTime()
                        || !team.getValidEndTime().after(new java.util.Date())
                        || team.getLockCount() >= team.getTargetCount();
                if (unavailable) {
                    log.info("交易锁单拦截-队伍不可加入 userId:{} teamId:{} activityId:{}", userId, teamId, activityId);
                    return Response.<LockMarketPayOrderResponseDTO>builder()
                            .code(ResponseCode.E0006.getCode())
                            .info("拼团队伍不存在、已结束或名额已满")
                            .build();
                }
            }

            // 使用服务端活动配置重新试算，得到当前有效的活动规则、商品价格和用户资格。
            TrialBalanceEntity trialBalanceEntity = indexGroupBuyMarketService.indexMarketTrial(MarketProductEntity.builder()
                    .userId(userId)
                    .source(source)
                    .channel(channel)
                    .goodsId(goodsId)
                    .activityId(activityId)
                    .build());

            // 人群标签可能控制“是否可见”和“是否可参与”；任一条件不满足都禁止锁单。
            if (!trialBalanceEntity.getIsVisible() || !trialBalanceEntity.getIsEnable()) {
                return Response.<LockMarketPayOrderResponseDTO>builder()
                        .code(ResponseCode.E0007.getCode())
                        .info(ResponseCode.E0007.getInfo())
                        .build();
            }

            GroupBuyActivityDiscountVO groupBuyActivityDiscountVO = trialBalanceEntity.getGroupBuyActivityDiscountVO();

            // 将试算结果拆分为用户、活动、优惠三个领域对象，交给锁单服务执行原子业务操作。
            marketPayOrderEntity = tradeOrderService.lockMarketPayOrder(
                    UserEntity.builder().userId(userId).build(),
                    PayActivityEntity.builder()
                            .teamId(teamId)
                            .activityId(activityId)
                            .activityName(groupBuyActivityDiscountVO.getActivityName())
                            .startTime(groupBuyActivityDiscountVO.getStartTime())
                            .endTime(groupBuyActivityDiscountVO.getEndTime())
                            .validTime(groupBuyActivityDiscountVO.getValidTime())
                            .targetCount(groupBuyActivityDiscountVO.getTarget())
                            .build(),
                    PayDiscountEntity.builder()
                            .source(source)
                            .channel(channel)
                            .goodsId(goodsId)
                            .goodsName(trialBalanceEntity.getGoodsName())
                            .originalPrice(trialBalanceEntity.getOriginalPrice())
                            .deductionPrice(trialBalanceEntity.getDeductionPrice())
                            .payPrice(trialBalanceEntity.getPayPrice())
                            .outTradeNo(outTradeNo)
                            .notifyConfigVO(
                                    // 将 API 层通知 DTO 转为领域值对象，随订单持久化供异步通知使用。
                                    NotifyConfigVO.builder()
                                            .notifyType(NotifyTypeEnumVO.valueOf(notifyConfigVO.getNotifyType()))
                                            .notifyMQ(notifyConfigVO.getNotifyMQ())
                                            .notifyUrl(notifyConfigVO.getNotifyUrl())
                                            .build())
                            .build());

            log.info("交易锁单记录(新):{} marketPayOrderEntity:{}", userId, JSON.toJSONString(marketPayOrderEntity));

            // 只返回调用方继续支付所需的信息，不直接暴露完整领域订单。
            return Response.<LockMarketPayOrderResponseDTO>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(LockMarketPayOrderResponseDTO.builder()
                            .orderId(marketPayOrderEntity.getOrderId())
                            .originalPrice(marketPayOrderEntity.getOriginalPrice())
                            .deductionPrice(marketPayOrderEntity.getDeductionPrice())
                            .payPrice(marketPayOrderEntity.getPayPrice())
                            .tradeOrderStatus(marketPayOrderEntity.getTradeOrderStatusEnumVO().getCode())
                            .teamId(marketPayOrderEntity.getTeamId())
                            .build())
                    .build();
        } catch (AppException e) {
            // 业务异常保留领域错误码，例如队伍已满、活动不可用等可识别失败。
            log.error("营销交易锁单业务异常:{} LockMarketPayOrderRequestDTO:{}", requestDTO.getUserId(), JSON.toJSONString(requestDTO), e);
            return Response.<LockMarketPayOrderResponseDTO>builder()
                    .code(e.getCode())
                    .info(e.getInfo())
                    .build();
        } catch (Exception e) {
            // 未预期异常统一收敛为系统错误，避免向调用方暴露内部实现细节。
            log.error("营销交易锁单服务失败:{} LockMarketPayOrderRequestDTO:{}", requestDTO.getUserId(), JSON.toJSONString(requestDTO), e);
            return Response.<LockMarketPayOrderResponseDTO>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

    /**
     * 接收外部支付成功结果并结算营销订单。
     *
     * <p>本接口不负责收款，而是在支付系统确认成功后推进拼团订单状态。领域服务会校验外部
     * 交易号对应的锁单记录，完成支付状态更新，并在达到目标人数时触发成团结算。</p>
     *
     * @param requestDTO 支付成功信息，包含用户、来源、渠道、外部交易号和支付时间
     * @return 结算后的用户、队伍、活动及外部交易标识
     */
    @RequestMapping(value = "settlement_market_pay_order", method = RequestMethod.POST)
    @Override
    public Response<SettlementMarketPayOrderResponseDTO> settlementMarketPayOrder(@RequestBody SettlementMarketPayOrderRequestDTO requestDTO) {
        try {
            log.info("营销交易组队结算开始:{} outTradeNo:{}", requestDTO.getUserId(), requestDTO.getOutTradeNo());

            // 支付时间是结算事实的一部分，与交易标识和渠道上下文一并校验。
            if (StringUtils.isBlank(requestDTO.getUserId()) || StringUtils.isBlank(requestDTO.getSource()) || StringUtils.isBlank(requestDTO.getChannel()) || StringUtils.isBlank(requestDTO.getOutTradeNo()) || null == requestDTO.getOutTradeTime()) {
                return Response.<SettlementMarketPayOrderResponseDTO>builder()
                        .code(ResponseCode.ILLEGAL_PARAMETER.getCode())
                        .info(ResponseCode.ILLEGAL_PARAMETER.getInfo())
                        .build();
            }

            // 将支付回调 DTO 转为领域成功事件，由结算服务推进订单和队伍状态。
            TradePaySettlementEntity tradePaySettlementEntity = tradeSettlementOrderService.settlementMarketPayOrder(TradePaySuccessEntity.builder()
                    .source(requestDTO.getSource())
                    .channel(requestDTO.getChannel())
                    .userId(requestDTO.getUserId())
                    .outTradeNo(requestDTO.getOutTradeNo())
                    .outTradeTime(requestDTO.getOutTradeTime())
                    .build());

            // 返回队伍与活动标识，便于支付方记录本次营销结算归属。
            SettlementMarketPayOrderResponseDTO responseDTO = SettlementMarketPayOrderResponseDTO.builder()
                    .userId(tradePaySettlementEntity.getUserId())
                    .teamId(tradePaySettlementEntity.getTeamId())
                    .activityId(tradePaySettlementEntity.getActivityId())
                    .outTradeNo(tradePaySettlementEntity.getOutTradeNo())
                    .build();

            // 领域结算完成后才构造成功响应。
            Response<SettlementMarketPayOrderResponseDTO> response = Response.<SettlementMarketPayOrderResponseDTO>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(responseDTO)
                    .build();

            log.info("营销交易组队结算完成:{} outTradeNo:{} response:{}", requestDTO.getUserId(), requestDTO.getOutTradeNo(), JSON.toJSONString(response));

            return response;
        } catch (AppException e) {
            // 可预期的结算失败直接透传领域错误码。
            log.error("营销交易组队结算异常:{} LockMarketPayOrderRequestDTO:{}", requestDTO.getUserId(), JSON.toJSONString(requestDTO), e);
            return Response.<SettlementMarketPayOrderResponseDTO>builder()
                    .code(e.getCode())
                    .info(e.getInfo())
                    .build();
        } catch (Exception e) {
            // 基础设施异常等非业务错误统一映射为系统错误。
            log.error("营销交易组队结算失败:{} LockMarketPayOrderRequestDTO:{}", requestDTO.getUserId(), JSON.toJSONString(requestDTO), e);
            return Response.<SettlementMarketPayOrderResponseDTO>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

    /**
     * 申请撤销指定的拼团营销订单。
     *
     * <p>退单服务会依据订单当前状态选择具体策略：未支付订单可直接释放锁单资源，已支付订单
     * 则需要记录相应退款行为。接口返回的行为码用于说明领域层最终采取的退单方式。</p>
     *
     * @param requestDTO 退单请求，使用用户 ID 和外部交易号定位订单，并携带来源、渠道上下文
     * @return 退单涉及的订单、队伍以及领域退单行为说明
     */
    @RequestMapping(value = "refund_market_pay_order", method = RequestMethod.POST)
    @Override
    public Response<RefundMarketPayOrderResponseDTO> refundMarketPayOrder(@RequestBody RefundMarketPayOrderRequestDTO requestDTO) {
        try {
            log.info("营销拼团退单开始:{} outTradeNo:{}", requestDTO.getUserId(), requestDTO.getOutTradeNo());

            // 定位订单和选择渠道处理逻辑所需的字段均不能为空。
            if (StringUtils.isBlank(requestDTO.getUserId()) || StringUtils.isBlank(requestDTO.getOutTradeNo()) || StringUtils.isBlank(requestDTO.getSource()) || StringUtils.isBlank(requestDTO.getChannel())) {
                return Response.<RefundMarketPayOrderResponseDTO>builder()
                        .code(ResponseCode.ILLEGAL_PARAMETER.getCode())
                        .info(ResponseCode.ILLEGAL_PARAMETER.getInfo())
                        .build();
            }

            // 将接口请求转换为退单命令，具体状态判断与资源恢复由领域服务完成。
            TradeRefundBehaviorEntity tradeRefundBehaviorEntity = tradeRefundOrderService.refundOrder(TradeRefundCommandEntity.builder()
                    .userId(requestDTO.getUserId())
                    .outTradeNo(requestDTO.getOutTradeNo())
                    .source(requestDTO.getSource())
                    .channel(requestDTO.getChannel())
                    .build());

            // 行为码和说明描述本次实际执行的退单策略，而非通用 HTTP 响应状态。
            RefundMarketPayOrderResponseDTO responseDTO = RefundMarketPayOrderResponseDTO.builder()
                    .userId(tradeRefundBehaviorEntity.getUserId())
                    .orderId(tradeRefundBehaviorEntity.getOrderId())
                    .teamId(tradeRefundBehaviorEntity.getTeamId())
                    .code(tradeRefundBehaviorEntity.getTradeRefundBehaviorEnum().getCode())
                    .info(tradeRefundBehaviorEntity.getTradeRefundBehaviorEnum().getInfo())
                    .build();

            // 领域退单成功执行后，外层响应仍使用统一成功码。
            Response<RefundMarketPayOrderResponseDTO> response = Response.<RefundMarketPayOrderResponseDTO>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(responseDTO)
                    .build();

            log.info("营销拼团退单完成:{} outTradeNo:{} response:{}", requestDTO.getUserId(), requestDTO.getOutTradeNo(), JSON.toJSONString(response));

            return response;
        } catch (AppException e) {
            // 订单不存在、状态不允许退单等业务错误按领域错误码返回。
            log.error("营销拼团退单异常:{} RefundMarketPayOrderRequestDTO:{}", requestDTO.getUserId(), JSON.toJSONString(requestDTO), e);
            return Response.<RefundMarketPayOrderResponseDTO>builder()
                    .code(e.getCode())
                    .info(e.getInfo())
                    .build();
        } catch (Exception e) {
            // 非预期错误统一映射，详细堆栈只记录在服务端日志中。
            log.error("营销拼团退单失败:{} RefundMarketPayOrderRequestDTO:{}", requestDTO.getUserId(), JSON.toJSONString(requestDTO), e);
            return Response.<RefundMarketPayOrderResponseDTO>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

}
