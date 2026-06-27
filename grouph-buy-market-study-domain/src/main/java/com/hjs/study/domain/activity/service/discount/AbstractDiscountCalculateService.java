package com.hjs.study.domain.activity.service.discount;

import com.hjs.study.domain.activity.model.valobj.DiscountTypeEnum;
import com.hjs.study.domain.activity.model.valobj.GroupBuyActivityDiscountVO;

import java.math.BigDecimal;

/**
 * 折扣计算模板方法。
 * 先执行通用的人群标签过滤，再交给子类处理具体营销公式。
 */
public abstract class AbstractDiscountCalculateService implements IDiscountCalculateService{
    @Override
    public BigDecimal calculate(String userId, BigDecimal originalPrice, GroupBuyActivityDiscountVO.GroupBuyDiscount groupBuyDiscount) {
        // 人群标签类型的优惠，只有命中配置人群后才继续计算。
        if (DiscountTypeEnum.TAG.equals(groupBuyDiscount.getDiscountType())){
            boolean isCrowdRange = filterTagId(userId, groupBuyDiscount.getTagId());
            if (!isCrowdRange)
                return originalPrice;
        }

        return doCalculate(originalPrice, groupBuyDiscount);
    }

    private boolean filterTagId(String userId,String tagId){

        // 预留人群标签命中查询逻辑。
        //TODO 后续开发
        return true;
    }

    protected abstract BigDecimal doCalculate(BigDecimal originalPrice, GroupBuyActivityDiscountVO.GroupBuyDiscount groupBuyDiscount);
}
