package com.hjs.study.domain.trade.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 交易域中的用户实体。
 * <p>
 * 这里刻意没有放昵称、手机号、头像等用户中心常见资料，
 * 因为交易域真正关心的只是“是谁在参与这笔交易”。
 * 这体现了 DDD 中“按上下文裁剪模型”的思想：
 * 同样是用户，在不同领域里需要的字段并不一样。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 用户实体对象
 * @create 2025-01-11 09:21
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserEntity {

    /** 用户唯一标识，是交易下单、支付、退款、查询参与次数的主维度。 */
    private String userId;

}
