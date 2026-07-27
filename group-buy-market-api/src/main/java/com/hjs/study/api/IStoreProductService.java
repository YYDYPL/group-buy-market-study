package com.hjs.study.api;

import com.hjs.study.api.dto.ProductConfigResponseDTO;
import com.hjs.study.api.dto.ProductPageResponseDTO;
import com.hjs.study.api.response.Response;

/**
 * 商城商品展示 API 契约。
 */
public interface IStoreProductService {

    Response<ProductPageResponseDTO> queryProducts(
            String keyword, String category, String sort, Integer page, Integer pageSize);

    Response<ProductConfigResponseDTO> queryProduct(String goodsId);
}
