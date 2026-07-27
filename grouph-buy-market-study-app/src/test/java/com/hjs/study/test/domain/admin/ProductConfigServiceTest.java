package com.hjs.study.test.domain.admin;

import com.hjs.study.domain.admin.model.entity.ProductConfigEntity;
import com.hjs.study.domain.admin.model.entity.ProductTrialEntity;
import com.hjs.study.domain.admin.service.ProductConfigService;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;

/**
 * 后台优惠试算规则测试。
 */
public class ProductConfigServiceTest {

    private final ProductConfigService service = new ProductConfigService();

    @Test
    public void shouldCalculateDirectReduction() {
        ProductTrialEntity result = service.trial(config("100.00", "ZJ", "20"));
        Assert.assertEquals(0, result.getPayPrice().compareTo(new BigDecimal("80.00")));
        Assert.assertEquals(0, result.getDeductionPrice().compareTo(new BigDecimal("20.00")));
    }

    @Test
    public void shouldCalculateThresholdReduction() {
        ProductTrialEntity result = service.trial(config("69.90", "MJ", "59,20"));
        Assert.assertEquals(0, result.getPayPrice().compareTo(new BigDecimal("49.90")));
    }

    @Test
    public void shouldCalculateFixedPrice() {
        ProductTrialEntity result = service.trial(config("39.90", "N", "19.90"));
        Assert.assertEquals(0, result.getPayPrice().compareTo(new BigDecimal("19.90")));
    }

    @Test
    public void shouldMatchExistingDiscountRounding() {
        ProductTrialEntity result = service.trial(config("159.00", "ZK", "0.69"));
        Assert.assertEquals(0, result.getPayPrice().compareTo(new BigDecimal("109")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectReductionNotLowerThanOriginalPrice() {
        service.trial(config("20.00", "ZJ", "20"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectUnreachableThreshold() {
        service.trial(config("50.00", "MJ", "59,20"));
    }

    private ProductConfigEntity config(String originalPrice, String plan, String expression) {
        return ProductConfigEntity.builder()
                .originalPrice(new BigDecimal(originalPrice))
                .marketPlan(plan)
                .marketExpr(expression)
                .build();
    }
}
