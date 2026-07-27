package com.hjs.study.test.trigger;

import com.hjs.study.api.dto.StoreOrderPageResponseDTO;
import com.hjs.study.api.dto.StoreTeamResponseDTO;
import com.hjs.study.api.response.Response;
import com.hjs.study.domain.trade.model.entity.StoreOrderEntity;
import com.hjs.study.domain.trade.model.entity.StoreTeamEntity;
import com.hjs.study.domain.trade.service.IStoreOrderQueryService;
import com.hjs.study.trigger.http.StoreOrderController;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.Date;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 商城订单与团队查询接口测试。
 */
public class StoreOrderControllerTest {

    @Test
    public void shouldExposePendingOrderActions() {
        IStoreOrderQueryService service = mock(IStoreOrderQueryService.class);
        when(service.queryUserOrders("hjs_a", null, 0, 1, 20))
                .thenReturn(Collections.singletonList(StoreOrderEntity.builder()
                        .userId("hjs_a")
                        .orderStatus(0)
                        .teamStatus(0)
                        .validEndTime(new Date(System.currentTimeMillis() + 60000))
                        .build()));
        when(service.countUserOrders("hjs_a", null, 0)).thenReturn(1);

        Response<StoreOrderPageResponseDTO> response =
                new StoreOrderController(service).queryUserOrders("hjs_a", null, 0, 1, 20);

        Assert.assertEquals("0000", response.getCode());
        Assert.assertEquals(Integer.valueOf(1), response.getData().getTotal());
        Assert.assertTrue(response.getData().getItems().get(0).getCanPay());
        Assert.assertTrue(response.getData().getItems().get(0).getCanCancel());
        Assert.assertFalse(response.getData().getItems().get(0).getCanRefund());
    }

    @Test
    public void shouldExposeCompletedRefundTeamAsHistoricalCompletion() {
        IStoreOrderQueryService service = mock(IStoreOrderQueryService.class);
        when(service.queryTeam("team_01")).thenReturn(StoreTeamEntity.builder()
                .teamId("team_01")
                .status(3)
                .targetCount(3)
                .completeCount(2)
                .lockCount(2)
                .validEndTime(new Date(System.currentTimeMillis() + 60000))
                .members(Collections.singletonList(StoreTeamEntity.Member.builder()
                        .userId("hjs_a")
                        .orderStatus(2)
                        .outTradeTime(new Date())
                        .build()))
                .build());

        Response<StoreTeamResponseDTO> response =
                new StoreOrderController(service).queryTeam("team_01");

        Assert.assertEquals("0000", response.getCode());
        Assert.assertEquals("完成-含退单", response.getData().getStatusText());
        Assert.assertFalse(response.getData().getCanJoin());
        Assert.assertEquals("已退款", response.getData().getMembers().get(0).getOrderStatusText());
    }
}
