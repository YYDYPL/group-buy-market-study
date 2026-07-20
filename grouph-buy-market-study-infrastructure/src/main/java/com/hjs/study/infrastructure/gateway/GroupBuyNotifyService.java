package com.hjs.study.infrastructure.gateway;


import com.hjs.study.types.enums.ResponseCode;
import com.hjs.study.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 拼团结果回调网关服务。
 * <p>
 * 该类负责把系统内部生成的拼团成功、退款等通知报文，
 * 通过 HTTP POST 的方式发送给外部业务系统。
 * 它本质上是一个基础设施网关组件，屏蔽了 OkHttp 的请求构造细节，
 * 让上层调用方只需要关注“回调地址”和“通知报文”即可。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 拼团回调服务
 * @create 2025-01-31 09:12
 */
@Slf4j
@Service
public class GroupBuyNotifyService {

    /** HTTP 客户端，由 Spring 容器统一配置连接池、超时等参数。 */
    @Resource
    private OkHttpClient okHttpClient;

    /**
     * 向外部系统发送拼团通知。
     * <p>
     * 当前统一使用 JSON 作为请求体格式，HTTP 方法固定为 POST。
     * 成功时直接返回对方接口的原始响应体，供上层继续判断业务结果；
     * 失败时统一包装为系统定义的 HTTP_EXCEPTION 异常码。
     *
     * @param apiUrl 外部回调地址
     * @param notifyRequestDTOJSON JSON 格式的通知报文
     * @return 外部接口返回的响应字符串
     * @throws Exception 调用失败时抛出统一异常
     */
    public String groupBuyNotify(String apiUrl, String notifyRequestDTOJSON) throws Exception {
        try {
            // 1. 构建参数
            MediaType mediaType = MediaType.parse("application/json");
            RequestBody body = RequestBody.create(mediaType, notifyRequestDTOJSON);
            Request request = new Request.Builder()
                    .url(apiUrl)
                    .post(body)
                    .addHeader("content-type", "application/json")
                    .build();

            // 2. 调用接口
            Response response = okHttpClient.newCall(request).execute();

            // 3. 返回结果
            return response.body().string();
        } catch (Exception e) {
            log.error("拼团回调 HTTP 接口服务异常 {}", apiUrl, e);
            throw new AppException(ResponseCode.HTTP_EXCEPTION);
        }
    }

}
