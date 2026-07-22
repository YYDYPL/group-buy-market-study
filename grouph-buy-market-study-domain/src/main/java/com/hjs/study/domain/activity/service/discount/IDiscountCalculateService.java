package com.hjs.study.domain.activity.service.discount;

import com.hjs.study.domain.activity.model.valobj.GroupBuyActivityDiscountVO;

import java.math.BigDecimal;
/**
 * 折扣计算策略接口。
 * <p>
 * 各种优惠方式虽然计算公式不同，但都遵循“输入原价和优惠配置，输出到手价”的统一协议。
 * 因此这里抽象出策略接口，再由 `ZJ`、`MJ`、`N`、`ZK` 等实现类各自完成具体计算。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 折扣计算服务
 * @create 2024-12-22 09:17
 */
public interface IDiscountCalculateService {

    /**
     * 折扣计算
     *
     * @param userId           用户ID
     * @param originalPrice    商品原始价格
     * @param groupBuyDiscount 折扣计划配置
     * @return 商品优惠价格
     */
    BigDecimal calculate(String userId, BigDecimal originalPrice, GroupBuyActivityDiscountVO.GroupBuyDiscount groupBuyDiscount);

}
