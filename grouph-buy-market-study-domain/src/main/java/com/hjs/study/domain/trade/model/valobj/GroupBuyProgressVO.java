package com.hjs.study.domain.trade.model.valobj;

import lombok.*;

/**
 * 拼团进度值对象。
 * <p>
 * 值对象强调“值语义”，不强调唯一身份。
 * 这里关注的不是“哪一支队伍”本身，而是“这支队伍当前的进度数值是多少”，
 * 因此使用值对象来承载目标数、完成数、锁单数这类可被整体理解的数据组合。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 拼团进度值对象
 * @create 2025-01-11 14:50
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GroupBuyProgressVO {

    /** 成团目标数量。 */
    private Integer targetCount;
    /** 已支付完成数量。 */
    private Integer completeCount;
    /** 已锁定名额但未必支付完成的数量。 */
    private Integer lockCount;

}
