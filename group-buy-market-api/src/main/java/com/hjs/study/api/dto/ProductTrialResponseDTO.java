package com.hjs.study.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 优惠试算响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductTrialResponseDTO {

    private BigDecimal originalPrice;
    private BigDecimal deductionPrice;
    private BigDecimal payPrice;
    private String explanation;
}
