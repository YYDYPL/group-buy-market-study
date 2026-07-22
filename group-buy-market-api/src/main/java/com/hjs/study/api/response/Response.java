package com.hjs.study.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * API 统一响应包装对象。
 *
 * <p>{@code code} 和 {@code info} 表达本次接口调用的业务结果，{@code data} 只在需要返回业务
 * 数据时使用。调用方不应只判断 HTTP 状态码，还应检查业务响应码。</p>
 *
 * @param <T> 成功响应携带的数据类型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Response<T> implements Serializable {

    /** 固定序列化版本，保证对象跨进程或持久化反序列化时具有稳定的版本标识。 */
    private static final long serialVersionUID = 7000723935764546321L;

    /** 业务响应码，例如 {@code 0000} 表示成功、{@code 0002} 表示参数非法。 */
    private String code;
    /** 与业务响应码对应的中文结果说明。 */
    private String info;
    /** 具体业务数据；失败响应或无需返回数据的成功响应中可以为空。 */
    private T data;

}
