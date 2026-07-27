package com.hjs.study.trigger.http.support;

import com.hjs.study.api.dto.ProductConfigRequestDTO;
import com.hjs.study.api.dto.ProductConfigResponseDTO;
import com.hjs.study.domain.admin.model.entity.ProductConfigEntity;
import com.hjs.study.domain.admin.model.entity.ProductTrialEntity;
import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.StringUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 商品配置的 HTTP DTO 与领域实体转换器。
 */
public final class ProductConfigAssembler {

    private static final String OUTPUT_DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";

    private ProductConfigAssembler() {
    }

    public static ProductConfigEntity toEntity(ProductConfigRequestDTO source) {
        return ProductConfigEntity.builder()
                .goodsId(source.getGoodsId())
                .goodsName(source.getGoodsName())
                .originalPrice(source.getOriginalPrice())
                .category(defaultString(source.getCategory(), "百货"))
                .subtitle(defaultString(source.getSubtitle(), ""))
                .mainImage(defaultString(source.getMainImage(), ""))
                .galleryImages(JSON.toJSONString(safeList(source.getGalleryImages())))
                .salesCount(defaultInteger(source.getSalesCount(), 0))
                .favorableRate(source.getFavorableRate())
                .serviceTags(JSON.toJSONString(safeList(source.getServiceTags())))
                .sortOrder(defaultInteger(source.getSortOrder(), 0))
                .productStatus(source.getProductStatus())
                .version(source.getVersion())
                .activityId(source.getActivityId())
                .activityName(source.getActivityName())
                .discountId(source.getDiscountId())
                .groupType(source.getGroupType())
                .takeLimitCount(source.getTakeLimitCount())
                .target(source.getTarget())
                .validTime(source.getValidTime())
                .startTime(parseDate(source.getStartTime()))
                .endTime(parseDate(source.getEndTime()))
                .discountName(source.getDiscountName())
                .discountDesc(defaultString(source.getDiscountDesc(), ""))
                .discountType(source.getDiscountType())
                .marketPlan(source.getMarketPlan())
                .marketExpr(source.getMarketExpr())
                .tagId(source.getTagId())
                .source(defaultString(source.getSource(), "s01"))
                .channel(defaultString(source.getChannel(), "c01"))
                .build();
    }

    public static ProductConfigResponseDTO toResponse(ProductConfigEntity source, ProductTrialEntity trial) {
        if (source == null) return null;
        return ProductConfigResponseDTO.builder()
                .goodsId(source.getGoodsId())
                .goodsName(source.getGoodsName())
                .originalPrice(source.getOriginalPrice())
                .deductionPrice(trial == null ? null : trial.getDeductionPrice())
                .payPrice(trial == null ? null : trial.getPayPrice())
                .priceExplanation(trial == null ? null : trial.getExplanation())
                .category(source.getCategory())
                .subtitle(source.getSubtitle())
                .mainImage(source.getMainImage())
                .galleryImages(parseList(source.getGalleryImages()))
                .salesCount(source.getSalesCount())
                .favorableRate(source.getFavorableRate())
                .serviceTags(parseList(source.getServiceTags()))
                .sortOrder(source.getSortOrder())
                .productStatus(source.getProductStatus())
                .version(source.getVersion())
                .activityId(source.getActivityId())
                .activityName(source.getActivityName())
                .discountId(source.getDiscountId())
                .groupType(source.getGroupType())
                .takeLimitCount(source.getTakeLimitCount())
                .target(source.getTarget())
                .validTime(source.getValidTime())
                .activityStatus(source.getActivityStatus())
                .startTime(formatDate(source.getStartTime()))
                .endTime(formatDate(source.getEndTime()))
                .discountName(source.getDiscountName())
                .discountDesc(source.getDiscountDesc())
                .discountType(source.getDiscountType())
                .marketPlan(source.getMarketPlan())
                .marketExpr(source.getMarketExpr())
                .tagId(source.getTagId())
                .source(source.getSource())
                .channel(source.getChannel())
                .build();
    }

    public static Date parseDate(String value) {
        if (StringUtils.isBlank(value)) return null;
        String[] patterns = {"yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd'T'HH:mm", OUTPUT_DATE_PATTERN};
        for (String pattern : patterns) {
            try {
                SimpleDateFormat format = new SimpleDateFormat(pattern);
                format.setLenient(false);
                return format.parse(value);
            } catch (ParseException ignored) {
                // 继续尝试下一种浏览器/接口常用格式。
            }
        }
        throw new IllegalArgumentException("时间格式不正确：" + value);
    }

    private static String formatDate(Date value) {
        return value == null ? null : new SimpleDateFormat(OUTPUT_DATE_PATTERN).format(value);
    }

    private static List<String> parseList(String json) {
        if (StringUtils.isBlank(json)) return new ArrayList<>();
        try {
            List<String> result = JSON.parseArray(json, String.class);
            return result == null ? new ArrayList<>() : result;
        } catch (Exception ignored) {
            return new ArrayList<>();
        }
    }

    private static List<String> safeList(List<String> source) {
        return source == null ? new ArrayList<>() : source;
    }

    private static String defaultString(String value, String fallback) {
        return value == null ? fallback : value;
    }

    private static Integer defaultInteger(Integer value, Integer fallback) {
        return value == null ? fallback : value;
    }
}
