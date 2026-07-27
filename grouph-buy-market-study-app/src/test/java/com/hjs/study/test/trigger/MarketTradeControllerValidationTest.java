package com.hjs.study.test.trigger;

import com.hjs.study.api.dto.LockMarketPayOrderRequestDTO;
import com.hjs.study.api.dto.LockMarketPayOrderResponseDTO;
import com.hjs.study.api.response.Response;
import com.hjs.study.domain.trade.model.entity.MarketPayOrderEntity;
import com.hjs.study.domain.trade.model.valobj.TradeOrderStatusEnumVO;
import com.hjs.study.domain.trade.service.ITradeLockOrderService;
import com.hjs.study.trigger.http.MarketTradeController;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 锁单接口参数与幂等响应测试。
 */
public class MarketTradeControllerValidationTest {

    @Test
    public void shouldRejectMissingNotifyConfigWithoutThrowing() {
        Response<LockMarketPayOrderResponseDTO> response =
                new MarketTradeController().lockMarketPayOrder(baseRequest());
        Assert.assertEquals("0002", response.getCode());
    }

    @Test
    public void shouldReturnTeamIdForIdempotentPendingOrder() {
        ITradeLockOrderService lockService = mock(ITradeLockOrderService.class);
        when(lockService.queryNoPayMarketPayOrderByOutTradeNo("hjs_a", "trade_01"))
                .thenReturn(MarketPayOrderEntity.builder()
                        .teamId("team_01")
                        .orderId("order_01")
                        .tradeOrderStatusEnumVO(TradeOrderStatusEnumVO.CREATE)
                        .build());

        MarketTradeController controller = new MarketTradeController();
        ReflectionTestUtils.setField(controller, "tradeOrderService", lockService);
        LockMarketPayOrderRequestDTO request = baseRequest();
        LockMarketPayOrderRequestDTO.NotifyConfigVO notify = new LockMarketPayOrderRequestDTO.NotifyConfigVO();
        notify.setNotifyType("MQ");
        request.setNotifyConfigVO(notify);

        Response<LockMarketPayOrderResponseDTO> response = controller.lockMarketPayOrder(request);

        Assert.assertEquals("0000", response.getCode());
        Assert.assertEquals("team_01", response.getData().getTeamId());
    }

    private LockMarketPayOrderRequestDTO baseRequest() {
        LockMarketPayOrderRequestDTO request = new LockMarketPayOrderRequestDTO();
        request.setUserId("hjs_a");
        request.setSource("s01");
        request.setChannel("c01");
        request.setGoodsId("9890001");
        request.setActivityId(100123L);
        request.setOutTradeNo("trade_01");
        return request;
    }
}
