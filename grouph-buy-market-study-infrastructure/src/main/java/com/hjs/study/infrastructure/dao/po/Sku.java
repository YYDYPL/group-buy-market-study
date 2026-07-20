package com.hjs.study.infrastructure.dao.po;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 商品基础信息表对应的 PO 对象。
 * <p>
 * 当前项目中的拼团活动最终仍然是围绕具体商品展开的，
 * 因此这里保存的是营销侧识别商品所需的最小信息集合：
 * 商品编号、名称、渠道维度以及原价。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 商品信息
 * @create 2024-12-21 10:45
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Sku {

        /** 数据库自增主键。 */
        private Long id;
        /** 渠道标识，用于区分不同业务入口或投放侧。 */
        private String source;
        /** 来源标识，和 source 组合后可定位一条具体商品配置。 */
        private String channel;
        /** 商品业务唯一 ID。 */
        private String goodsId;
        /** 商品名称，用于页面展示与后台配置。 */
        private String goodsName;
        /** 商品原价，是营销优惠计算前的基准价格。 */
        private BigDecimal originalPrice;
        /** 记录创建时间。 */
        private Date createTime;
        /** 记录更新时间。 */
        private Date updateTime;

}
