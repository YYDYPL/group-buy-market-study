package com.hjs.study.infrastructure.dao.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 渠道商品活动关联表对应的 PO 对象。
 * <p>
 * 这张表负责把“渠道 + 来源 + 商品”三元组映射到具体的拼团活动上，
 * 是用户进入营销链路时定位活动配置的重要入口。
 * 也就是说，先确定商品和渠道维度，再通过这里找到对应的 activityId。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 渠道商品活动配置关联表
 * @create 2025-01-01 09:27
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SCSkuActivity {

    /** 数据库自增主键。 */
    private Long id;
    /** 渠道标识，例如不同投放平台或业务入口。 */
    private String source;
    /** 来源标识，通常与 source 组合形成更细粒度的路由维度。 */
    private String channel;
    /** 关联的拼团活动业务 ID。 */
    private Long activityId;
    /** 关联的商品业务 ID。 */
    private String goodsId;
    /** 记录创建时间。 */
    private Date createTime;
    /** 记录更新时间。 */
    private Date updateTime;

}
