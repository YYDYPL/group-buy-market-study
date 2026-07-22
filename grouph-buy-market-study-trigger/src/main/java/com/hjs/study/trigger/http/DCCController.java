package com.hjs.study.trigger.http;

import com.hjs.study.api.IDCCService;
import com.hjs.study.api.response.Response;
import com.hjs.study.types.enums.ResponseCode;
import cn.bugstack.wrench.dynamic.config.center.domain.model.valobj.AttributeVO;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RTopic;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * 动态配置中心（DCC）的 HTTP 触发器。
 *
 * <p>本类只负责接收配置变更请求，并将键值对发布到 Redis Topic。真正的配置绑定、
 * 本地属性刷新等工作由动态配置中心组件的订阅端完成，因此这里不直接修改任何业务 Bean。</p>
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @create 2025-01-03 19:16
 */
@Slf4j
@RestController()
@CrossOrigin("*")
@RequestMapping("/api/v1/gbm/dcc/")
public class DCCController implements IDCCService {

    /**
     * 动态配置变更消息的 Redis 发布主题。
     *
     * <p>通过 Bean 名称精确注入，避免应用中存在多个 {@link RTopic} 实例时发生歧义。</p>
     */
    @Resource(name = "dynamicConfigCenterRedisTopic")
    private RTopic dccTopic;

    /**
     * 发布一条动态配置变更消息。
     *
     * <p>接口返回成功仅表示消息已成功发布到 Redis Topic；配置是否被各应用实例完成消费，
     * 由订阅端的处理结果决定。</p>
     * <p>
     * 调用示例：
     * <pre>
     * curl http://127.0.0.1:8091/api/v1/gbm/dcc/update_config?key=downgradeSwitch&value=1
     * curl http://127.0.0.1:8091/api/v1/gbm/dcc/update_config?key=cutRange&value=0
     * curl http://127.0.0.1:8091/api/v1/gbm/dcc/update_config?key=rateLimiterSwitch&value=close
     * </pre>
     *
     * @param key   待变更的配置项名称，应与动态配置组件中声明的属性名一致
     * @param value 配置项的新值，订阅端会按目标属性类型完成转换
     * @return 发布成功时返回成功响应；出现异常时返回统一的系统错误响应
     */
    @RequestMapping(value = "update_config", method = RequestMethod.GET)
    @Override
    public Response<Boolean> updateConfig(@RequestParam String key, @RequestParam String value) {
        try {
            log.info("DCC 动态配置值变更 key:{} value:{}", key, value);
            // AttributeVO 是配置中心约定的消息载体，订阅端根据 key 定位属性并写入 value。
            dccTopic.publish(new AttributeVO(key, value));
            return Response.<Boolean>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .build();
        } catch (Exception e) {
            log.error("DCC 动态配置值变更失败 key:{} value:{}", key, value, e);
            return Response.<Boolean>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

}
