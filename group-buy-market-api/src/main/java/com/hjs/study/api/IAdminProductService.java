package com.hjs.study.api;

import com.hjs.study.api.dto.ProductConfigRequestDTO;
import com.hjs.study.api.dto.ProductConfigResponseDTO;
import com.hjs.study.api.dto.ProductPageResponseDTO;
import com.hjs.study.api.dto.ProductTrialResponseDTO;
import com.hjs.study.api.response.Response;

/**
 * 运营后台商品配置 API 契约。
 */
public interface IAdminProductService {

    Response<Boolean> verify();

    Response<ProductPageResponseDTO> queryProducts(String keyword, Integer status, Integer page, Integer pageSize);

    Response<ProductConfigResponseDTO> queryProduct(String goodsId);

    Response<ProductTrialResponseDTO> trial(ProductConfigRequestDTO requestDTO);

    Response<ProductConfigResponseDTO> saveDraft(ProductConfigRequestDTO requestDTO);

    Response<ProductConfigResponseDTO> publish(String goodsId, Integer version);

    Response<ProductConfigResponseDTO> offline(String goodsId, Integer version);

    Response<ProductConfigResponseDTO> abandon(String goodsId, Integer version);
}
