package com.hjs.study.trigger.http;

import com.hjs.study.api.IStoreOrderService;
import com.hjs.study.api.dto.StoreOrderPageResponseDTO;
import com.hjs.study.api.dto.StoreOrderResponseDTO;
import com.hjs.study.api.dto.StoreTeamResponseDTO;
import com.hjs.study.api.response.Response;
import com.hjs.study.domain.trade.model.entity.StoreOrderEntity;
import com.hjs.study.domain.trade.model.entity.StoreTeamEntity;
import com.hjs.study.domain.trade.service.IStoreOrderQueryService;
import com.hjs.study.types.enums.GroupBuyOrderEnumVO;
import com.hjs.study.types.enums.ResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 商城订单中心与团队详情查询接口。
 */
@Slf4j
@RestController
@CrossOrigin("*")
@RequestMapping("/api/v1/gbm/store")
public class StoreOrderController implements IStoreOrderService {

    private final IStoreOrderQueryService storeOrderQueryService;

    public StoreOrderController(IStoreOrderQueryService storeOrderQueryService) {
        this.storeOrderQueryService = storeOrderQueryService;
    }

    @Override
    @GetMapping("/users/{userId}/orders")
    public Response<StoreOrderPageResponseDTO> queryUserOrders(
            @PathVariable String userId,
            @RequestParam(required = false) String goodsId,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        if (StringUtils.isBlank(userId) || (status != null && (status < 0 || status > 2))) {
            return illegal("用户或订单状态参数不正确");
        }
        int safePage = Math.max(page == null ? 1 : page, 1);
        int safePageSize = Math.max(1, Math.min(pageSize == null ? 20 : pageSize, 50));
        try {
            List<StoreOrderResponseDTO> items = new ArrayList<>();
            for (StoreOrderEntity entity : storeOrderQueryService.queryUserOrders(
                    userId, goodsId, status, safePage, safePageSize)) {
                items.add(toOrderResponse(entity));
            }
            return success(StoreOrderPageResponseDTO.builder()
                    .items(items)
                    .total(storeOrderQueryService.countUserOrders(userId, goodsId, status))
                    .page(safePage)
                    .pageSize(safePageSize)
                    .build());
        } catch (Exception e) {
            log.error("查询商城订单失败 userId:{}", userId, e);
            return failure("商城订单查询失败");
        }
    }

    @Override
    @GetMapping("/teams/{teamId}")
    public Response<StoreTeamResponseDTO> queryTeam(@PathVariable String teamId) {
        if (StringUtils.isBlank(teamId)) return illegal("团队ID不能为空");
        try {
            StoreTeamEntity team = storeOrderQueryService.queryTeam(teamId);
            if (team == null) return illegal("拼团队伍不存在");

            List<StoreTeamResponseDTO.Member> members = new ArrayList<>();
            if (team.getMembers() != null) {
                for (StoreTeamEntity.Member member : team.getMembers()) {
                    members.add(StoreTeamResponseDTO.Member.builder()
                            .userId(member.getUserId())
                            .orderId(member.getOrderId())
                            .outTradeNo(member.getOutTradeNo())
                            .orderStatus(member.getOrderStatus())
                            .orderStatusText(orderStatusText(member.getOrderStatus(), member.getOutTradeTime()))
                            .outTradeTime(member.getOutTradeTime())
                            .createTime(member.getCreateTime())
                            .build());
                }
            }
            boolean canJoin = Integer.valueOf(0).equals(team.getStatus())
                    && team.getValidEndTime() != null
                    && team.getValidEndTime().after(new Date())
                    && team.getLockCount() < team.getTargetCount();
            return success(StoreTeamResponseDTO.builder()
                    .teamId(team.getTeamId())
                    .activityId(team.getActivityId())
                    .source(team.getSource())
                    .channel(team.getChannel())
                    .originalPrice(team.getOriginalPrice())
                    .deductionPrice(team.getDeductionPrice())
                    .payPrice(team.getPayPrice())
                    .targetCount(team.getTargetCount())
                    .completeCount(team.getCompleteCount())
                    .lockCount(team.getLockCount())
                    .status(team.getStatus())
                    .statusText(teamStatusText(team.getStatus()))
                    .validStartTime(team.getValidStartTime())
                    .validEndTime(team.getValidEndTime())
                    .canJoin(canJoin)
                    .members(members)
                    .build());
        } catch (Exception e) {
            log.error("查询拼团队伍失败 teamId:{}", teamId, e);
            return failure("拼团队伍查询失败");
        }
    }

    private StoreOrderResponseDTO toOrderResponse(StoreOrderEntity entity) {
        boolean teamInProgress = Integer.valueOf(0).equals(entity.getTeamStatus());
        boolean teamCanSettle = teamInProgress && entity.getValidEndTime() != null
                && entity.getValidEndTime().after(new Date());
        boolean paidRefundable = Integer.valueOf(1).equals(entity.getOrderStatus())
                && (teamInProgress || Integer.valueOf(1).equals(entity.getTeamStatus())
                || Integer.valueOf(3).equals(entity.getTeamStatus()));
        return StoreOrderResponseDTO.builder()
                .userId(entity.getUserId())
                .orderId(entity.getOrderId())
                .outTradeNo(entity.getOutTradeNo())
                .goodsId(entity.getGoodsId())
                .goodsName(entity.getGoodsName())
                .mainImage(entity.getMainImage())
                .activityId(entity.getActivityId())
                .teamId(entity.getTeamId())
                .source(entity.getSource())
                .channel(entity.getChannel())
                .originalPrice(entity.getOriginalPrice())
                .deductionPrice(entity.getDeductionPrice())
                .payPrice(entity.getPayPrice())
                .orderStatus(entity.getOrderStatus())
                .orderStatusText(orderStatusText(entity.getOrderStatus(), entity.getOutTradeTime()))
                .outTradeTime(entity.getOutTradeTime())
                .createTime(entity.getCreateTime())
                .updateTime(entity.getUpdateTime())
                .teamStatus(entity.getTeamStatus())
                .teamStatusText(teamStatusText(entity.getTeamStatus()))
                .targetCount(entity.getTargetCount())
                .completeCount(entity.getCompleteCount())
                .lockCount(entity.getLockCount())
                .validEndTime(entity.getValidEndTime())
                .canPay(Integer.valueOf(0).equals(entity.getOrderStatus()) && teamCanSettle)
                .canCancel(Integer.valueOf(0).equals(entity.getOrderStatus()) && teamInProgress)
                .canRefund(paidRefundable)
                .build();
    }

    private String orderStatusText(Integer status, Date outTradeTime) {
        if (Integer.valueOf(0).equals(status)) return "待支付";
        if (Integer.valueOf(1).equals(status)) return "已支付";
        if (Integer.valueOf(2).equals(status)) return outTradeTime == null ? "已取消" : "已退款";
        return "未知状态";
    }

    private String teamStatusText(Integer status) {
        try {
            return GroupBuyOrderEnumVO.valueOf(status).getInfo();
        } catch (Exception ignored) {
            return "未知状态";
        }
    }

    private <T> Response<T> success(T data) {
        return Response.<T>builder()
                .code(ResponseCode.SUCCESS.getCode())
                .info(ResponseCode.SUCCESS.getInfo())
                .data(data)
                .build();
    }

    private <T> Response<T> illegal(String info) {
        return Response.<T>builder().code(ResponseCode.ILLEGAL_PARAMETER.getCode()).info(info).build();
    }

    private <T> Response<T> failure(String info) {
        return Response.<T>builder().code(ResponseCode.UN_ERROR.getCode()).info(info).build();
    }
}
