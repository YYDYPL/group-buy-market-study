package com.hjs.study.domain.activity.service.discount;

import com.hjs.study.domain.activity.adapter.repository.IActivityRepository;
import com.hjs.study.domain.activity.model.valobj.DiscountTypeEnum;
import com.hjs.study.domain.activity.model.valobj.GroupBuyActivityDiscountVO;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.math.BigDecimal;

/**
 * 折扣计算模板抽象类。
 * <p>
 * 它把所有优惠方式共用的流程先抽出来：
 * 1. 如果优惠受标签限制，则先做人群过滤；
 * 2. 过滤通过后，再交给子类做真正的价格计算。
 * 这样各优惠实现只需要关心各自公式，不必重复写标签判断逻辑。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 折扣计算服务抽象类
 * @create 2024-12-22 12:32
 */
@Slf4j
public abstract class AbstractDiscountCalculateService implements IDiscountCalculateService {

    /** 活动域仓储，用于查询标签命中情况。 */
    @Resource
    protected IActivityRepository repository;

    @Override
    /**
     * 执行统一折扣计算流程。
     *
     * @param userId 用户 ID
     * @param originalPrice 原价
     * @param groupBuyDiscount 折扣配置
     * @return 优惠后的支付价格
     */
    public BigDecimal calculate(String userId, BigDecimal originalPrice, GroupBuyActivityDiscountVO.GroupBuyDiscount groupBuyDiscount) {
        // 1. 人群标签过滤
        if (DiscountTypeEnum.TAG.equals(groupBuyDiscount.getDiscountType())){
            boolean isCrowdRange = filterTagId(userId, groupBuyDiscount.getTagId());
            if (!isCrowdRange) {
                log.info("折扣优惠计算拦截，用户不再优惠人群标签范围内 userId:{}", userId);
                return originalPrice;
            }
        }
        // 2. 折扣优惠计算
        return doCalculate(originalPrice, groupBuyDiscount);
    }

    /** 人群过滤，判断指定用户是否符合标签优惠范围。 */
    private boolean filterTagId(String userId, String tagId) {
        return repository.isTagCrowdRange(tagId, userId);
    }

    /**
     * 子类实现自己的折扣公式。
     *
     * @param originalPrice 原价
     * @param groupBuyDiscount 折扣配置
     * @return 计算后的支付金额
     */
    protected abstract BigDecimal doCalculate(BigDecimal originalPrice, GroupBuyActivityDiscountVO.GroupBuyDiscount groupBuyDiscount);

}
