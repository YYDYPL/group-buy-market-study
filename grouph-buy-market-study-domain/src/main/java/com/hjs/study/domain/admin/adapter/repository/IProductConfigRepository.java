package com.hjs.study.domain.admin.adapter.repository;

import com.hjs.study.domain.admin.model.entity.ProductConfigEntity;

import java.util.List;

/**
 * 运营后台商品配置仓储。
 */
public interface IProductConfigRepository {

    List<ProductConfigEntity> queryStoreProducts(
            String keyword, String category, String sort, int offset, int limit);

    int countStoreProducts(String keyword, String category);

    List<ProductConfigEntity> queryAdminProducts(String keyword, Integer status, int offset, int limit);

    int countAdminProducts(String keyword, Integer status);

    ProductConfigEntity queryProductConfig(String goodsId);

    ProductConfigEntity queryStoreProduct(String goodsId);

    ProductConfigEntity saveDraft(ProductConfigEntity entity);

    ProductConfigEntity publish(String goodsId, Integer expectedVersion);

    ProductConfigEntity offline(String goodsId, Integer expectedVersion);

    ProductConfigEntity abandon(String goodsId, Integer expectedVersion);
}
