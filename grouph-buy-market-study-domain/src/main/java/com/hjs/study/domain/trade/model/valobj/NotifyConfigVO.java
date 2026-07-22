package com.hjs.study.domain.trade.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 回调配置值对象。
 * <p>
 * 它描述的是“系统应该如何把交易结果通知给外部”这组不可再拆的配置值，
 * 包括通知方式、消息主题、回调地址等。
 * 之所以建模为值对象，是因为它没有独立业务身份，
 * 只要内部字段相同，就可以认为是同一份通知配置语义。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 回调配置值对象
 * @create 2025-03-16 16:10
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotifyConfigVO {

    /** 回调方式枚举，用于决定后续通知走 MQ 还是 HTTP。 */
    private NotifyTypeEnumVO notifyType;
    /** MQ 方式下使用的消息主题或队列标识。 */
    private String notifyMQ;
    /** HTTP 方式下使用的回调地址。 */
    private String notifyUrl;

}
