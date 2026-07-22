package com.hjs.study.domain.activity.service.discount.impl;

import com.hjs.study.domain.activity.model.valobj.GroupBuyActivityDiscountVO;
import com.hjs.study.domain.activity.service.discount.AbstractDiscountCalculateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 折扣优惠计算器。
 * <p>
 * 该模式按折扣系数计算最终价格，
 * 例如表达式为 `0.8` 时表示按原价的 8 折计算。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 折扣优惠计算
 * @create 2024-12-22 12:12
 */
@Slf4j
@Service("ZK")
public class ZKCalculateService extends AbstractDiscountCalculateService {

    @Override
    /** 按折扣系数乘以原价，并向下取整到分。 */
    public BigDecimal doCalculate(BigDecimal originalPrice, GroupBuyActivityDiscountVO.GroupBuyDiscount groupBuyDiscount) {
        log.info("优惠策略折扣计算:{}", groupBuyDiscount.getDiscountType().getCode());

        // 折扣表达式 - 折扣百分比
        String marketExpr = groupBuyDiscount.getMarketExpr();

        // 折扣价格 + 四舍五入
        BigDecimal deductionPrice = originalPrice.multiply(new BigDecimal(marketExpr)).setScale(0, RoundingMode.DOWN);

        // 判断折扣后金额，最低支付1分钱
        if (deductionPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return new BigDecimal("0.01");
        }

        return deductionPrice;
    }

}

