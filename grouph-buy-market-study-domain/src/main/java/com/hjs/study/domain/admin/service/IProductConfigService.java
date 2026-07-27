package com.hjs.study.domain.admin.service;

import com.hjs.study.domain.admin.model.entity.ProductConfigEntity;
import com.hjs.study.domain.admin.model.entity.ProductTrialEntity;

import java.util.List;

/**
 * 商城展示和运营后台共同使用的商品配置服务。
 */
public interface IProductConfigService {

    List<ProductConfigEntity> queryStoreProducts(String keyword, String category, String sort, int page, int pageSize);

    int countStoreProducts(String keyword, String category);

    List<ProductConfigEntity> queryAdminProducts(String keyword, Integer status, int page, int pageSize);

    int countAdminProducts(String keyword, Integer status);

    ProductConfigEntity queryProductConfig(String goodsId);

    ProductConfigEntity queryStoreProduct(String goodsId);

    ProductTrialEntity trial(ProductConfigEntity entity);

    ProductConfigEntity saveDraft(ProductConfigEntity entity);

    ProductConfigEntity publish(String goodsId, Integer expectedVersion);

    ProductConfigEntity offline(String goodsId, Integer expectedVersion);

    ProductConfigEntity abandon(String goodsId, Integer expectedVersion);
}
