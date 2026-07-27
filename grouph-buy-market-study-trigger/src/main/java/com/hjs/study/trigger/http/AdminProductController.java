package com.hjs.study.trigger.http;

import com.hjs.study.api.IAdminProductService;
import com.hjs.study.api.dto.ProductConfigRequestDTO;
import com.hjs.study.api.dto.ProductConfigResponseDTO;
import com.hjs.study.api.dto.ProductPageResponseDTO;
import com.hjs.study.api.dto.ProductTrialResponseDTO;
import com.hjs.study.api.response.Response;
import com.hjs.study.domain.admin.model.entity.ProductConfigEntity;
import com.hjs.study.domain.admin.model.entity.ProductTrialEntity;
import com.hjs.study.domain.admin.service.IProductConfigService;
import com.hjs.study.trigger.http.support.ProductConfigAssembler;
import com.hjs.study.types.enums.ResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 拼团商城运营后台 HTTP 入口。
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/gbm/admin")
public class AdminProductController implements IAdminProductService {

    @Resource
    private IProductConfigService productConfigService;

    @Override
    @GetMapping("/auth/verify")
    public Response<Boolean> verify() {
        return success(true);
    }

    @Override
    @GetMapping("/products")
    public Response<ProductPageResponseDTO> queryProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "12") Integer pageSize) {
        try {
            List<ProductConfigResponseDTO> items = new ArrayList<>();
            for (ProductConfigEntity entity : productConfigService.queryAdminProducts(keyword, status, page, pageSize)) {
                items.add(toResponse(entity));
            }
            return success(ProductPageResponseDTO.builder()
                    .items(items)
                    .total(productConfigService.countAdminProducts(keyword, status))
                    .page(Math.max(page, 1))
                    .pageSize(Math.max(1, Math.min(pageSize, 50)))
                    .build());
        } catch (Exception e) {
            return failure(e);
        }
    }

    @Override
    @GetMapping("/products/{goodsId}")
    public Response<ProductConfigResponseDTO> queryProduct(@PathVariable String goodsId) {
        try {
            ProductConfigEntity entity = productConfigService.queryProductConfig(goodsId);
            if (entity == null) return illegal("商品配置不存在");
            return success(toResponse(entity));
        } catch (Exception e) {
            return failure(e);
        }
    }

    @Override
    @PostMapping("/products/trial")
    public Response<ProductTrialResponseDTO> trial(@RequestBody ProductConfigRequestDTO requestDTO) {
        try {
            ProductTrialEntity trial = productConfigService.trial(ProductConfigAssembler.toEntity(requestDTO));
            return success(ProductTrialResponseDTO.builder()
                    .originalPrice(trial.getOriginalPrice())
                    .deductionPrice(trial.getDeductionPrice())
                    .payPrice(trial.getPayPrice())
                    .explanation(trial.getExplanation())
                    .build());
        } catch (Exception e) {
            return failure(e);
        }
    }

    @Override
    @PostMapping("/products/draft")
    public Response<ProductConfigResponseDTO> saveDraft(@RequestBody ProductConfigRequestDTO requestDTO) {
        try {
            ProductConfigEntity saved = productConfigService.saveDraft(ProductConfigAssembler.toEntity(requestDTO));
            return success(toResponse(saved));
        } catch (Exception e) {
            return failure(e);
        }
    }

    @Override
    @PostMapping("/products/{goodsId}/publish")
    public Response<ProductConfigResponseDTO> publish(@PathVariable String goodsId, @RequestParam Integer version) {
        try {
            return success(toResponse(productConfigService.publish(goodsId, version)));
        } catch (Exception e) {
            return failure(e);
        }
    }

    @Override
    @PostMapping("/products/{goodsId}/offline")
    public Response<ProductConfigResponseDTO> offline(@PathVariable String goodsId, @RequestParam Integer version) {
        try {
            return success(toResponse(productConfigService.offline(goodsId, version)));
        } catch (Exception e) {
            return failure(e);
        }
    }

    @Override
    @PostMapping("/products/{goodsId}/abandon")
    public Response<ProductConfigResponseDTO> abandon(@PathVariable String goodsId, @RequestParam Integer version) {
        try {
            return success(toResponse(productConfigService.abandon(goodsId, version)));
        } catch (Exception e) {
            return failure(e);
        }
    }

    private ProductConfigResponseDTO toResponse(ProductConfigEntity entity) {
        ProductTrialEntity trial = null;
        if (entity != null && entity.getOriginalPrice() != null
                && entity.getMarketPlan() != null && entity.getMarketExpr() != null) {
            try {
                trial = productConfigService.trial(entity);
            } catch (Exception ignored) {
                // 草稿可能暂时不完整，详情仍应可打开并继续编辑。
            }
        }
        return ProductConfigAssembler.toResponse(entity, trial);
    }

    private <T> Response<T> success(T data) {
        return Response.<T>builder()
                .code(ResponseCode.SUCCESS.getCode())
                .info(ResponseCode.SUCCESS.getInfo())
                .data(data)
                .build();
    }

    private <T> Response<T> illegal(String message) {
        return Response.<T>builder()
                .code(ResponseCode.ILLEGAL_PARAMETER.getCode())
                .info(message)
                .build();
    }

    private <T> Response<T> failure(Exception e) {
        log.warn("运营后台请求处理失败", e);
        if (e instanceof IllegalStateException) {
            return Response.<T>builder().code(ResponseCode.CONFLICT.getCode()).info(e.getMessage()).build();
        }
        if (e instanceof IllegalArgumentException) {
            return illegal(e.getMessage());
        }
        return Response.<T>builder().code(ResponseCode.UN_ERROR.getCode()).info("系统处理失败").build();
    }
}
