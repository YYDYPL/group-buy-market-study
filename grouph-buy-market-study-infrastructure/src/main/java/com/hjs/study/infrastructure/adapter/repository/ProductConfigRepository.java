package com.hjs.study.infrastructure.adapter.repository;

import com.hjs.study.domain.admin.adapter.repository.IProductConfigRepository;
import com.hjs.study.domain.admin.model.entity.ProductConfigEntity;
import com.hjs.study.infrastructure.dao.IProductConfigDao;
import com.hjs.study.infrastructure.dao.po.GroupBuyActivity;
import com.hjs.study.infrastructure.dao.po.GroupBuyDiscount;
import com.hjs.study.infrastructure.dao.po.ProductConfig;
import com.hjs.study.infrastructure.redis.IRedisService;
import com.alibaba.fastjson.JSON;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 后台配置仓储实现。
 */
@Repository
public class ProductConfigRepository implements IProductConfigRepository {

    @Resource
    private IProductConfigDao productConfigDao;
    @Resource
    private IRedisService redisService;

    @Override
    public List<ProductConfigEntity> queryStoreProducts(
            String keyword, String category, String sort, int offset, int limit) {
        ProductConfig condition = ProductConfig.builder()
                .keyword(keyword).category(category).sort(sort).offset(offset).pageSize(limit).build();
        return convert(productConfigDao.queryStoreProductList(condition));
    }

    @Override
    public int countStoreProducts(String keyword, String category) {
        return productConfigDao.countStoreProductList(ProductConfig.builder().keyword(keyword).category(category).build());
    }

    @Override
    public List<ProductConfigEntity> queryAdminProducts(String keyword, Integer status, int offset, int limit) {
        ProductConfig condition = ProductConfig.builder()
                .keyword(keyword).status(status).offset(offset).pageSize(limit).build();
        return convert(productConfigDao.queryAdminProductList(condition));
    }

    @Override
    public int countAdminProducts(String keyword, Integer status) {
        return productConfigDao.countAdminProductList(ProductConfig.builder().keyword(keyword).status(status).build());
    }

    @Override
    public ProductConfigEntity queryProductConfig(String goodsId) {
        return toEntity(productConfigDao.queryProductConfigByGoodsId(goodsId));
    }

    @Override
    public ProductConfigEntity queryStoreProduct(String goodsId) {
        return toEntity(productConfigDao.queryStoreProductByGoodsId(goodsId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductConfigEntity saveDraft(ProductConfigEntity entity) {
        ProductConfig config = toPo(entity);
        ProductConfig existing = productConfigDao.querySkuByGoodsId(entity.getGoodsId());
        if (existing == null) {
            config.setVersion(0);
            if (productConfigDao.insertSku(config) != 1) throw new IllegalStateException("商品草稿新增失败");
        } else {
            if (productConfigDao.updateSkuDraft(config) != 1) {
                throw new IllegalStateException("配置已被其他页面修改，请刷新后重试");
            }
            config.setVersion(config.getVersion() + 1);
        }

        /*
         * 商品资料与活动规则一起写入活动快照。已上架商品在保存草稿时只推进
         * 乐观锁版本，不改动 sku 的线上资料；真正发布时再一次性切换。
         */
        config.setDraftData(JSON.toJSONString(config));
        productConfigDao.abandonOldDrafts(config.getGoodsId());
        if (productConfigDao.insertDiscount(config) != 1) throw new IllegalStateException("优惠草稿新增失败");
        config.setActivityStatus(0);
        if (productConfigDao.insertActivity(config) != 1) throw new IllegalStateException("活动草稿新增失败");
        return toEntity(productConfigDao.queryProductConfigByGoodsId(config.getGoodsId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductConfigEntity publish(String goodsId, Integer expectedVersion) {
        ProductConfig draft = productConfigDao.queryDraftByGoodsId(goodsId);
        if (draft == null) throw new IllegalStateException("没有可发布的活动草稿");
        draft.setVersion(expectedVersion);
        draft.setProductStatus(1);

        if (productConfigDao.updateSkuPublish(draft) != 1) {
            throw new IllegalStateException("配置已被其他页面修改，请刷新后重试");
        }

        ProductConfig active = productConfigDao.queryActiveByGoodsId(goodsId);
        if (active != null && !active.getActivityId().equals(draft.getActivityId())) {
            active.setActivityStatus(2);
            productConfigDao.updateActivityStatus(active);
            active.setStatus(0);
            productConfigDao.updateRouteStatus(active);
        }

        draft.setActivityStatus(1);
        if (productConfigDao.updateActivityStatus(draft) != 1) throw new IllegalStateException("活动发布失败");
        if (productConfigDao.upsertRoute(draft) < 1) throw new IllegalStateException("渠道路由发布失败");

        evict(active);
        evict(draft);
        return toEntity(productConfigDao.queryProductConfigByGoodsId(goodsId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductConfigEntity offline(String goodsId, Integer expectedVersion) {
        ProductConfig active = productConfigDao.queryActiveByGoodsId(goodsId);
        if (active == null) throw new IllegalStateException("商品当前没有已发布活动");
        active.setVersion(expectedVersion);
        active.setProductStatus(2);
        if (productConfigDao.updateSkuStatus(active) != 1) {
            throw new IllegalStateException("配置已被其他页面修改，请刷新后重试");
        }
        active.setActivityStatus(2);
        productConfigDao.updateActivityStatus(active);
        active.setStatus(0);
        productConfigDao.updateRouteStatus(active);
        evict(active);
        return toEntity(productConfigDao.queryProductConfigByGoodsId(goodsId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductConfigEntity abandon(String goodsId, Integer expectedVersion) {
        ProductConfig draft = productConfigDao.queryDraftByGoodsId(goodsId);
        if (draft == null) throw new IllegalStateException("商品当前没有草稿");
        ProductConfig sku = productConfigDao.querySkuByGoodsId(goodsId);
        sku.setVersion(expectedVersion);
        sku.setProductStatus(sku.getProductStatus());
        if (productConfigDao.updateSkuStatus(sku) != 1) {
            throw new IllegalStateException("配置已被其他页面修改，请刷新后重试");
        }
        draft.setActivityStatus(3);
        productConfigDao.updateActivityStatus(draft);
        evict(draft);
        return toEntity(productConfigDao.queryProductConfigByGoodsId(goodsId));
    }

    private void evict(ProductConfig config) {
        if (config == null) return;
        if (config.getActivityId() != null) redisService.remove(GroupBuyActivity.cacheRedisKey(config.getActivityId()));
        if (config.getDiscountId() != null) redisService.remove(GroupBuyDiscount.cacheRedisKey(config.getDiscountId()));
    }

    private List<ProductConfigEntity> convert(List<ProductConfig> list) {
        if (list == null || list.isEmpty()) return Collections.emptyList();
        return list.stream().map(this::toEntity).collect(Collectors.toList());
    }

    private ProductConfigEntity toEntity(ProductConfig source) {
        if (source == null) return null;
        ProductConfigEntity target = new ProductConfigEntity();
        BeanUtils.copyProperties(source, target);
        return target;
    }

    private ProductConfig toPo(ProductConfigEntity source) {
        ProductConfig target = new ProductConfig();
        BeanUtils.copyProperties(source, target);
        return target;
    }
}
