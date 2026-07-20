package com.hjs.study.infrastructure.dao;

import com.hjs.study.infrastructure.dao.po.SCSkuActivity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 渠道商品活动关联 Mapper。
 * <p>
 * 该接口对应 {@code sc_sku_activity} 表，
 * 用于根据“渠道 + 来源 + 商品”三元组找到对应的拼团活动。
 * 它是活动试算与锁单流程的入口路由数据之一。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 渠道商品活动配置关联表Dao
 * @create 2025-01-01 09:30
 */
@Mapper
public interface ISCSkuActivityDao {

    /**
     * 查询指定渠道商品对应的活动配置。
     *
     * @param scSkuActivity 查询条件，通常包含 {@code source}、{@code channel}、{@code goodsId}
     * @return 命中的渠道商品活动关系；未命中时返回 {@code null}
     */
    SCSkuActivity querySCSkuActivityBySCGoodsId(SCSkuActivity scSkuActivity);

}
