package com.hjs.study.infrastructure.dao;


import com.hjs.study.infrastructure.dao.po.Sku;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品基础信息 Mapper。
 * <p>
 * 该接口对应 {@code sku} 表，用于按商品业务 ID 读取营销侧所需的商品基础数据，
 * 例如商品名称、原价以及关联的渠道维度信息。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 商品查询
 * @create 2024-12-21 10:48
 */
@Mapper
public interface ISkuDao {

    /**
     * 按商品业务 ID 查询商品信息。
     *
     * @param goodsId 商品业务 ID
     * @return 商品信息；未命中时返回 {@code null}
     */
    Sku querySkuByGoodsId(String goodsId);

}
