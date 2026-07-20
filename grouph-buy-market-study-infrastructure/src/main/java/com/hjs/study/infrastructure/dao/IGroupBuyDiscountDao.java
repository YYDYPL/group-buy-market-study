package com.hjs.study.infrastructure.dao;

import com.hjs.study.infrastructure.dao.po.GroupBuyDiscount;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 拼团折扣配置 Mapper。
 * <p>
 * 该接口面向 {@code group_buy_discount} 表，
 * 用于读取活动关联的折扣方案与营销表达式配置。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 折扣配置Dao
 * @create 2024-12-07 10:10
 */
@Mapper
public interface IGroupBuyDiscountDao {

    /**
     * 查询全部折扣配置。
     *
     * @return 折扣配置列表
     */
    List<GroupBuyDiscount> queryGroupBuyDiscountList();

    /**
     * 按折扣业务 ID 查询折扣配置。
     *
     * @param discountId 折扣业务 ID
     * @return 折扣配置；未命中时返回 {@code null}
     */
    GroupBuyDiscount queryGroupBuyActivityDiscountByDiscountId(String discountId);

}
