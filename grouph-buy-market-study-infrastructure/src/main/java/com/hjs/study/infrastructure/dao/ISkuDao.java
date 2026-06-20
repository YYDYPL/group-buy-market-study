package com.hjs.study.infrastructure.dao;


import com.hjs.study.infrastructure.dao.po.Sku;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ISkuDao {

    Sku querySkuByGoodsId(String goodsId);

}