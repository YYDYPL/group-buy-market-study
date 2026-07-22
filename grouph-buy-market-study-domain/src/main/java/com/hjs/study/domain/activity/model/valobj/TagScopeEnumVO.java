package com.hjs.study.domain.activity.model.valobj;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.index.qual.NegativeIndexFor;

/**
 * 活动标签作用域枚举。
 * <p>
 * 该枚举不是在描述“某个具体标签”，而是在描述：
 * 当活动配置了标签限制后，这个限制作用于“可见性”还是“参与资格”。
 * 也就是说，它定义的是标签规则的生效位置。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 活动人群标签作用域范围枚举
 * @create 2025-01-02 10:58
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum TagScopeEnumVO {

    /** 控制用户是否能看到该拼团活动入口。 */
    VISIBLE(true,false,"是否可看见拼团"),
    /** 控制用户是否能真正参与下单或进团。 */
    ENABLE(true, false,"是否可参与拼团"),
    ;

    private Boolean allow;
    private Boolean refuse;
    private String desc;

}
