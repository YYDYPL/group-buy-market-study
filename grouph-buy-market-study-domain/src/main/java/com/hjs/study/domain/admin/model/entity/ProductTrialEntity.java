package com.hjs.study.domain.admin.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 后台优惠试算结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductTrialEntity {

    private BigDecimal originalPrice;
    private BigDecimal deductionPrice;
    private BigDecimal payPrice;
    private String explanation;
}
