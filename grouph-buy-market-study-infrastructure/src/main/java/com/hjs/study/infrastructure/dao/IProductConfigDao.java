package com.hjs.study.infrastructure.dao;

import com.hjs.study.infrastructure.dao.po.ProductConfig;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 商品、活动、优惠和路由的后台聚合 Mapper。
 */
@Mapper
public interface IProductConfigDao {

    List<ProductConfig> queryStoreProductList(ProductConfig condition);

    int countStoreProductList(ProductConfig condition);

    List<ProductConfig> queryAdminProductList(ProductConfig condition);

    int countAdminProductList(ProductConfig condition);

    ProductConfig queryProductConfigByGoodsId(String goodsId);

    ProductConfig queryStoreProductByGoodsId(String goodsId);

    ProductConfig querySkuByGoodsId(String goodsId);

    ProductConfig queryDraftByGoodsId(String goodsId);

    ProductConfig queryActiveByGoodsId(String goodsId);

    int insertSku(ProductConfig config);

    int updateSkuDraft(ProductConfig config);

    int updateSkuPublish(ProductConfig config);

    int updateSkuStatus(ProductConfig config);

    int abandonOldDrafts(String goodsId);

    int insertDiscount(ProductConfig config);

    int insertActivity(ProductConfig config);

    int updateActivityStatus(ProductConfig config);

    int upsertRoute(ProductConfig config);

    int updateRouteStatus(ProductConfig config);
}
