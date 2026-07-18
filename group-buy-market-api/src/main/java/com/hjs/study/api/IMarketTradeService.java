package com.hjs.study.api;

import com.hjs.study.api.dto.LockMarketPayOrderRequestDTO;
import com.hjs.study.api.dto.LockMarketPayOrderResponseDTO;
import com.hjs.study.api.response.Response;

public interface IMarketTradeService {

    Response<LockMarketPayOrderResponseDTO> lockMarketPayOrder(LockMarketPayOrderRequestDTO lockMarketPayOrderRequestDTO);

}
