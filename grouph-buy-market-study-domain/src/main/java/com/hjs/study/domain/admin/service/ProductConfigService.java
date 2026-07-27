package com.hjs.study.domain.admin.service;

import com.hjs.study.domain.admin.adapter.repository.IProductConfigRepository;
import com.hjs.study.domain.admin.model.entity.ProductConfigEntity;
import com.hjs.study.domain.admin.model.entity.ProductTrialEntity;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * 商品配置领域服务。
 *
 * <p>这里集中校验优惠表达式和活动规则。后台试算与保存草稿复用同一套公式，
 * 可以在发布前发现“满减门槛高于原价”等不可用配置。</p>
 */
@Service
public class ProductConfigService implements IProductConfigService {

    @Resource
    private IProductConfigRepository repository;

    @Override
    public List<ProductConfigEntity> queryStoreProducts(
            String keyword, String category, String sort, int page, int pageSize) {
        return repository.queryStoreProducts(
                keyword, category, sort, offset(page, pageSize), normalizePageSize(pageSize));
    }

    @Override
    public int countStoreProducts(String keyword, String category) {
        return repository.countStoreProducts(keyword, category);
    }

    @Override
    public List<ProductConfigEntity> queryAdminProducts(String keyword, Integer status, int page, int pageSize) {
        return repository.queryAdminProducts(keyword, status, offset(page, pageSize), normalizePageSize(pageSize));
    }

    @Override
    public int countAdminProducts(String keyword, Integer status) {
        return repository.countAdminProducts(keyword, status);
    }

    @Override
    public ProductConfigEntity queryProductConfig(String goodsId) {
        if (StringUtils.isBlank(goodsId)) throw new IllegalArgumentException("商品ID不能为空");
        return repository.queryProductConfig(goodsId);
    }

    @Override
    public ProductConfigEntity queryStoreProduct(String goodsId) {
        if (StringUtils.isBlank(goodsId)) throw new IllegalArgumentException("商品ID不能为空");
        return repository.queryStoreProduct(goodsId);
    }

    @Override
    public ProductTrialEntity trial(ProductConfigEntity entity) {
        validateDiscount(entity);
        BigDecimal original = entity.getOriginalPrice();
        BigDecimal payPrice;
        String explanation;

        switch (entity.getMarketPlan()) {
            case "ZJ":
                BigDecimal reduction = decimal(entity.getMarketExpr(), "直减金额");
                if (reduction.compareTo(original) >= 0) throw new IllegalArgumentException("直减金额必须小于商品原价");
                payPrice = original.subtract(reduction);
                explanation = "原价 " + original + " 元，拼团直减 " + reduction + " 元";
                break;
            case "MJ":
                String[] expression = entity.getMarketExpr().split(",");
                if (expression.length != 2) throw new IllegalArgumentException("满减表达式应为“门槛,减免金额”");
                BigDecimal threshold = decimal(expression[0], "满减门槛");
                BigDecimal reduce = decimal(expression[1], "满减金额");
                if (original.compareTo(threshold) < 0) throw new IllegalArgumentException("商品原价未达到满减门槛");
                if (reduce.compareTo(original) >= 0) throw new IllegalArgumentException("满减金额必须小于商品原价");
                payPrice = original.subtract(reduce);
                explanation = "满 " + threshold + " 元减 " + reduce + " 元";
                break;
            case "N":
                payPrice = decimal(entity.getMarketExpr(), "N元购价格");
                if (payPrice.compareTo(original) >= 0) throw new IllegalArgumentException("N元购价格必须低于商品原价");
                explanation = "拼团固定价 " + payPrice + " 元";
                break;
            case "ZK":
                BigDecimal rate = decimal(entity.getMarketExpr(), "折扣系数");
                if (rate.compareTo(BigDecimal.ZERO) <= 0 || rate.compareTo(BigDecimal.ONE) >= 0) {
                    throw new IllegalArgumentException("折扣系数必须大于0且小于1");
                }
                // 与现有 ZKCalculateService 保持完全一致：向下取整到元。
                payPrice = original.multiply(rate).setScale(0, RoundingMode.DOWN);
                explanation = "拼团享 " + rate.multiply(BigDecimal.TEN).stripTrailingZeros().toPlainString() + " 折";
                break;
            default:
                throw new IllegalArgumentException("不支持的优惠策略：" + entity.getMarketPlan());
        }

        if (payPrice.compareTo(new BigDecimal("0.01")) < 0) {
            throw new IllegalArgumentException("最终拼团价不能低于0.01元");
        }

        return ProductTrialEntity.builder()
                .originalPrice(original)
                .deductionPrice(original.subtract(payPrice))
                .payPrice(payPrice)
                .explanation(explanation)
                .build();
    }

    @Override
    public ProductConfigEntity saveDraft(ProductConfigEntity entity) {
        validateProduct(entity, false);
        validateActivity(entity, false);
        trial(entity);

        // 已发布配置不可覆盖；每次保存都生成一组新的活动/优惠业务 ID，
        // 原草稿会在同一事务中转为废弃状态，从而保留清晰的配置版本链。
        entity.setActivityId(UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE);
        entity.setDiscountId(UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase());
        entity.setActivityStatus(0);
        if (entity.getProductStatus() == null) entity.setProductStatus(0);
        if (entity.getVersion() == null) entity.setVersion(0);
        if (StringUtils.isBlank(entity.getSource())) entity.setSource("s01");
        if (StringUtils.isBlank(entity.getChannel())) entity.setChannel("c01");
        if (entity.getGroupType() == null) entity.setGroupType(0);
        if (entity.getDiscountType() == null) entity.setDiscountType(0);
        return repository.saveDraft(entity);
    }

    @Override
    public ProductConfigEntity publish(String goodsId, Integer expectedVersion) {
        ProductConfigEntity entity = requireConfig(goodsId, expectedVersion);
        validateProduct(entity, true);
        validateActivity(entity, true);
        trial(entity);
        return repository.publish(goodsId, expectedVersion);
    }

    @Override
    public ProductConfigEntity offline(String goodsId, Integer expectedVersion) {
        requireConfig(goodsId, expectedVersion);
        return repository.offline(goodsId, expectedVersion);
    }

    @Override
    public ProductConfigEntity abandon(String goodsId, Integer expectedVersion) {
        requireConfig(goodsId, expectedVersion);
        return repository.abandon(goodsId, expectedVersion);
    }

    private ProductConfigEntity requireConfig(String goodsId, Integer expectedVersion) {
        if (StringUtils.isBlank(goodsId) || expectedVersion == null) {
            throw new IllegalArgumentException("商品ID和版本号不能为空");
        }
        ProductConfigEntity entity = repository.queryProductConfig(goodsId);
        if (entity == null) throw new IllegalArgumentException("商品配置不存在");
        if (!expectedVersion.equals(entity.getVersion())) throw new IllegalStateException("配置已被其他页面修改，请刷新后重试");
        return entity;
    }

    private void validateProduct(ProductConfigEntity entity, boolean publishing) {
        if (entity == null) throw new IllegalArgumentException("商品配置不能为空");
        if (StringUtils.isBlank(entity.getGoodsId()) || entity.getGoodsId().length() > 16) {
            throw new IllegalArgumentException("商品ID不能为空且长度不能超过16位");
        }
        if (StringUtils.isBlank(entity.getGoodsName())) throw new IllegalArgumentException("商品名称不能为空");
        if (entity.getOriginalPrice() == null || entity.getOriginalPrice().compareTo(new BigDecimal("0.01")) < 0) {
            throw new IllegalArgumentException("商品原价不能低于0.01元");
        }
        if (publishing && StringUtils.isBlank(entity.getMainImage())) throw new IllegalArgumentException("发布前必须配置商品主图");
        if (entity.getFavorableRate() != null
                && (entity.getFavorableRate().compareTo(BigDecimal.ZERO) < 0
                || entity.getFavorableRate().compareTo(new BigDecimal("100")) > 0)) {
            throw new IllegalArgumentException("好评率必须在0到100之间");
        }
    }

    private void validateActivity(ProductConfigEntity entity, boolean publishing) {
        if (StringUtils.isBlank(entity.getActivityName())) throw new IllegalArgumentException("活动名称不能为空");
        if (StringUtils.isBlank(entity.getMarketPlan()) || StringUtils.isBlank(entity.getMarketExpr())) {
            throw new IllegalArgumentException("优惠策略和表达式不能为空");
        }
        if (entity.getTarget() == null || entity.getTarget() < 2) throw new IllegalArgumentException("成团人数至少为2人");
        if (entity.getTakeLimitCount() == null || entity.getTakeLimitCount() <= 0) {
            throw new IllegalArgumentException("参团次数必须为正数");
        }
        if (entity.getValidTime() == null || entity.getValidTime() <= 0) {
            throw new IllegalArgumentException("队伍有效期必须为正数");
        }
        Date start = entity.getStartTime();
        Date end = entity.getEndTime();
        if (start == null || end == null || !end.after(start)) throw new IllegalArgumentException("活动结束时间必须晚于开始时间");
        if (publishing && (StringUtils.isBlank(entity.getSource()) || StringUtils.isBlank(entity.getChannel()))) {
            throw new IllegalArgumentException("发布前必须配置来源和渠道");
        }
    }

    private void validateDiscount(ProductConfigEntity entity) {
        if (entity == null || entity.getOriginalPrice() == null) throw new IllegalArgumentException("商品原价不能为空");
        if (StringUtils.isBlank(entity.getMarketPlan()) || StringUtils.isBlank(entity.getMarketExpr())) {
            throw new IllegalArgumentException("优惠策略和表达式不能为空");
        }
    }

    private BigDecimal decimal(String value, String name) {
        try {
            BigDecimal result = new BigDecimal(value.trim());
            if (result.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException(name + "必须大于0");
            return result;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(name + "格式不正确");
        }
    }

    private int normalizePageSize(int pageSize) {
        return Math.max(1, Math.min(pageSize, 50));
    }

    private int offset(int page, int pageSize) {
        return (Math.max(page, 1) - 1) * normalizePageSize(pageSize);
    }
}
