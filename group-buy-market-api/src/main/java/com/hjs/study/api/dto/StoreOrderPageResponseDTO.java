package com.hjs.study.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 商城用户订单分页响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreOrderPageResponseDTO {

    private List<StoreOrderResponseDTO> items;
    private Integer total;
    private Integer page;
    private Integer pageSize;
}
