package com.hjs.study.trigger.http;

import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.nacos.api.config.ConfigService;
import com.hjs.study.api.IDCCService;
import com.hjs.study.api.response.Response;
import com.hjs.study.types.enums.ResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 动态配置中心（DCC）的 HTTP 触发器。
 *
 * <p>本类只负责接收配置变更请求，并将键值对发布到 Nacos 配置中心。真正的配置绑定、
 * 本地属性刷新由 Spring Cloud Alibaba Nacos Config 客户端订阅机制完成：
 * 当 Nacos 上的 dataId 内容变化时，会触发 {@link RefreshScope} 标记的 Bean 重建，
 * 因此这里不直接修改任何业务 Bean。</p>
 *
 * <p>与历史实现（xfg-wrench + Redis Topic）的差异：
 * <ul>
 *   <li>原通过 Redis {@code RTopic.publish(AttributeVO)} 广播配置变更；</li>
 *   <li>现在通过 Nacos {@code ConfigService.publishConfig(dataId, group, content, type)} 落库；</li>
 *   <li>客户端通过 Nacos 长轮询感知变更，进而触发 {@code @RefreshScope} 重建。</li>
 * </ul>
 *
 * <p>调用示例：
 * <pre>
 * curl 'http://127.0.0.1:8091/api/v1/gbm/dcc/update_config?key=downgradeSwitch&value=1'
 * curl 'http://127.0.0.1:8091/api/v1/gbm/dcc/update_config?key=cutRange&value=0'
 * curl 'http://127.0.0.1:8091/api/v1/gbm/dcc/update_config?key=cacheSwitch&value=1'
 * </pre>
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
     * Nacos dataId 中动态配置所在的命名空间前缀。
     * <p>所有通过本接口发布的配置项都会被收敛到 {@code dcc.*} 命名空间，
     * 避免与 Spring 内置属性或其他业务配置冲突。
     */
    private static final String DCC_PREFIX = "dcc.";

    /**
     * Nacos 配置文件扩展名，与 bootstrap.yml 中 {@code spring.cloud.nacos.config.file-extension} 保持一致。
     */
    private static final String FILE_EXTENSION = "yaml";

    /**
     * Spring Cloud Alibaba 自动装配的 Nacos 配置管理器，可获取已与 Nacos Server 建立连接的 {@link ConfigService}。
     * <p>避免直接 new ConfigService 实例，保证与 Spring Cloud 上下文使用同一客户端、同一长轮询通道。
     */
    @Resource
    private NacosConfigManager nacosConfigManager;

    /**
     * 当前 Spring 应用名；与 bootstrap.yml 中 {@code spring.application.name} 一致，用于拼接 dataId。
     */
    @Value("${spring.application.name}")
    private String applicationName;

    /**
     * 当前激活的 profile；用于拼接 dataId（{@code ${appName}-${profile}.yaml}）。
     */
    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    /**
     * Nacos 配置分组；与 bootstrap.yml 中 {@code spring.cloud.nacos.config.group} 一致。
     */
    @Value("${spring.cloud.nacos.config.group:DEFAULT_GROUP}")
    private String group;

    /**
     * 发布一条动态配置变更消息到 Nacos。
     *
     * <p>实现步骤：
     * <ol>
     *   <li>按 dataId + group 从 Nacos 拉取当前配置内容；</li>
     *   <li>用 SnakeYAML 解析为 Map，定位到 {@code dcc} 节点并更新指定 key；</li>
     *   <li>整体重新序列化为 YAML 并调 {@link ConfigService#publishConfig} 落库；</li>
     *   <li>Nacos 服务端会异步推送给所有订阅客户端，触发 {@code @RefreshScope} 刷新。</li>
     * </ol>
     * <p>
     * 接口返回成功仅表示 Nacos 端配置已经更新成功；客户端是否完成刷新由各自订阅周期决定。
     *
     * @param key   待变更的配置项名称；调用方无需带 {@code dcc.} 前缀，由本类内部统一加前
     * @param value 配置项的新值，以字符串形式传输
     * @return 发布成功时返回成功响应；出现异常时返回统一的系统错误响应
     */
    @RequestMapping(value = "update_config", method = RequestMethod.GET)
    @Override
    public Response<Boolean> updateConfig(@RequestParam String key, @RequestParam String value) {
        try {
            log.info("DCC 动态配置值变更（Nacos） key:{} value:{}", key, value);

            ConfigService configService = nacosConfigManager.getConfigService();
            String dataId = applicationName + "-" + activeProfile + "." + FILE_EXTENSION;

            // 1. 拉取当前配置内容；首次发布时可能为空。
            String content = configService.getConfig(dataId, group, 5000L);

            // 2. 解析为 Map；空内容时给一个空 Map 作为起点，避免 NPE。
            Map<String, Object> root = parseYaml(content);

            // 3. 定位到 dcc 节点并更新 key；保持嵌套结构以便序列化输出更清晰。
            @SuppressWarnings("unchecked")
            Map<String, Object> dccNode = (Map<String, Object>) root.computeIfAbsent(
                    "dcc", k -> new LinkedHashMap<String, Object>());
            dccNode.put(key, value);

            // 4. 重新序列化为 YAML 并发布。
            String newContent = dumpYaml(root);
            boolean published = configService.publishConfig(dataId, group, newContent, FILE_EXTENSION);

            log.info("DCC 动态配置发布完成 dataId:{} group:{} published:{}", dataId, group, published);

            if (published) {
                return Response.<Boolean>builder()
                        .code(ResponseCode.SUCCESS.getCode())
                        .info(ResponseCode.SUCCESS.getInfo())
                        .build();
            }
            return Response.<Boolean>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        } catch (Exception e) {
            log.error("DCC 动态配置值变更失败（Nacos） key:{} value:{}", key, value, e);
            return Response.<Boolean>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

    /**
     * 将 YAML 字符串解析为 Map；空字符串或 null 时返回空 Map。
     * <p>使用 Spring Boot 自带的 SnakeYAML，避免引入额外依赖。
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseYaml(String content) {
        if (!StringUtils.hasText(content)) {
            return new LinkedHashMap<>();
        }
        org.yaml.snakeyaml.Yaml yaml = new org.yaml.snakeyaml.Yaml();
        Object loaded = yaml.load(content);
        if (loaded == null) {
            return new LinkedHashMap<>();
        }
        if (loaded instanceof Map) {
            return new LinkedHashMap<>((Map<String, Object>) loaded);
        }
        // 兜底：内容不是 Map 结构（极少出现），返回空 Map 避免覆盖既有配置。
        return new LinkedHashMap<>();
    }

    /**
     * 将 Map 序列化为 YAML 字符串。
     * <p>{@code dumpAsMap} 保证输出为标准的 YAML 映射结构，便于在 Nacos 控制台查看与人工编辑。
     */
    private String dumpYaml(Map<String, Object> root) {
        org.yaml.snakeyaml.Yaml yaml = new org.yaml.snakeyaml.Yaml();
        return yaml.dumpAsMap(root);
    }

}
