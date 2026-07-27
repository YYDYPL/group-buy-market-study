package com.hjs.study.api;

import com.hjs.study.api.response.Response;

/**
 * DCC（Dynamic Configuration Center）动态配置服务契约。
 *
 * <p>用于在应用运行期间发布配置项变更，例如降级开关、切量范围和限流开关。接口只描述
 * 配置变更协议，具体发布与订阅机制由 Trigger 及动态配置组件实现（当前由 Nacos 承载）。</p>
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @create 2025-01-03 19:16
 */
public interface IDCCService {

    /**
     * 发布指定配置项的新值。
     *
     * <p>返回成功通常表示配置变更消息已经成功发布，不等价于所有应用实例均已完成异步刷新。</p>
     *
     * @param key   配置项名称，应与动态配置组件声明的属性名保持一致
     * @param value 配置项新值，以字符串形式传输，由订阅端转换为目标属性类型
     * @return 配置发布结果；业务状态由外层 {@link Response} 的 code 和 info 表达
     */
    Response<Boolean> updateConfig(String key, String value);

}
