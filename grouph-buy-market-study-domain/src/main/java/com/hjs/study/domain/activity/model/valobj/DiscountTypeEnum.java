package com.hjs.study.domain.activity.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 折扣限定类型枚举。
 * <p>
 * 它描述的不是“具体怎么算优惠”，而是“这条优惠规则对哪些用户生效”。
 * 例如基础优惠对所有人开放，标签优惠则只有命中指定人群标签的用户才可享受。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 折扣优惠类型
 * @create 2024-12-22 12:37
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum DiscountTypeEnum {

    /** 不附带人群条件，所有满足活动条件的用户都可享受。 */
    BASE(0, "基础优惠"),
    /** 只有命中指定标签人群的用户才可享受该优惠。 */
    TAG(1, "人群标签"),
    ;

    private Integer code;
    private String info;

    /**
     * 根据数据库中的数值编码转换为枚举对象。
     *
     * @param code 折扣限定类型编码
     * @return 对应枚举
     */
    public static DiscountTypeEnum get(Integer code) {
        switch (code) {
            case 0:
                return BASE;
            case 1:
                return TAG;
            default:
                throw new RuntimeException("err code!");
        }
    }

}