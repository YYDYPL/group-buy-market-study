package com.hjs.study.trigger.http;

import com.hjs.study.api.IStoreProductService;
import com.hjs.study.api.dto.ProductConfigResponseDTO;
import com.hjs.study.api.dto.ProductPageResponseDTO;
import com.hjs.study.api.response.Response;
import com.hjs.study.domain.admin.model.entity.ProductConfigEntity;
import com.hjs.study.domain.admin.model.entity.ProductTrialEntity;
import com.hjs.study.domain.admin.service.IProductConfigService;
import com.hjs.study.trigger.http.support.ProductConfigAssembler;
import com.hjs.study.types.enums.ResponseCode;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 商城公开商品目录接口。
 */
@RestController
@CrossOrigin("*")
@RequestMapping("/api/v1/gbm/store")
public class StoreProductController implements IStoreProductService {

    @Resource
    private IProductConfigService productConfigService;

    @Override
    @GetMapping("/products")
    public Response<ProductPageResponseDTO> queryProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "default") String sort,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        try {
            List<ProductConfigResponseDTO> items = new ArrayList<>();
            for (ProductConfigEntity entity : productConfigService.queryStoreProducts(
                    keyword, category, sort, page, pageSize)) {
                items.add(toResponse(entity));
            }
            return success(ProductPageResponseDTO.builder()
                    .items(items)
                    .total(productConfigService.countStoreProducts(keyword, category))
                    .page(Math.max(page, 1))
                    .pageSize(Math.max(1, Math.min(pageSize, 50)))
                    .build());
        } catch (Exception e) {
            return failure();
        }
    }

    @Override
    @GetMapping("/products/{goodsId}")
    public Response<ProductConfigResponseDTO> queryProduct(@PathVariable String goodsId) {
        try {
            ProductConfigEntity entity = productConfigService.queryStoreProduct(goodsId);
            if (entity == null || !Integer.valueOf(1).equals(entity.getProductStatus())
                    || !Integer.valueOf(1).equals(entity.getActivityStatus())) {
                return Response.<ProductConfigResponseDTO>builder()
                        .code(ResponseCode.ILLEGAL_PARAMETER.getCode())
                        .info("商品不存在或已经下架")
                        .build();
            }
            return success(toResponse(entity));
        } catch (Exception e) {
            return failure();
        }
    }

    private ProductConfigResponseDTO toResponse(ProductConfigEntity entity) {
        ProductTrialEntity trial = productConfigService.trial(entity);
        return ProductConfigAssembler.toResponse(entity, trial);
    }

    private <T> Response<T> success(T data) {
        return Response.<T>builder().code(ResponseCode.SUCCESS.getCode()).info(ResponseCode.SUCCESS.getInfo()).data(data).build();
    }

    private <T> Response<T> failure() {
        return Response.<T>builder().code(ResponseCode.UN_ERROR.getCode()).info("商城商品查询失败").build();
    }
}
