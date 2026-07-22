package com.hjs.study.domain.activity.service.discount.impl;

import com.hjs.study.domain.activity.model.valobj.GroupBuyActivityDiscountVO;
import com.hjs.study.domain.activity.service.discount.AbstractDiscountCalculateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * N 元购优惠计算器。
 * <p>
 * 这类优惠最直接，营销表达式本身就是最终支付金额，
 * 例如配置 `9.9` 就表示商品统一按 9.9 元购买。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description N元购优惠计算
 * @create 2024-12-22 12:12
 */
@Slf4j
@Service("N")
public class NCalculateService extends AbstractDiscountCalculateService {

    @Override
    /** 直接把营销表达式解析为最终支付价格。 */
    public BigDecimal doCalculate(BigDecimal originalPrice, GroupBuyActivityDiscountVO.GroupBuyDiscount groupBuyDiscount) {
        log.info("优惠策略折扣计算:{}", groupBuyDiscount.getDiscountType().getCode());

        // 折扣表达式 - 直接为优惠后的金额
        String marketExpr = groupBuyDiscount.getMarketExpr();
        // n元购
        return new BigDecimal(marketExpr);
    }

}
