package com.hjs.study.domain.trade.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 回调方式枚举。
 * <p>
 * 该枚举定义交易域支持的外部通知通道，不同枚举值会驱动不同的基础设施实现。
 * 例如 HTTP 走接口调用，MQ 走消息投递。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 回调方式枚举
 * @create 2025-03-16 15:47
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public enum NotifyTypeEnumVO {

    /** 基于 HTTP 接口的同步/异步回调。 */
    HTTP("HTTP", "HTTP 回调"),
    /** 基于消息队列的事件通知。 */
    MQ("MQ", "MQ 消息通知"),
    ;

    private String code;
    private String info;

}
