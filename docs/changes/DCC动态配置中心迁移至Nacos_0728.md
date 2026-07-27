# DCC 动态配置中心迁移至 Nacos

## 变更信息

- 变更时间：2026-07-28 04:38:00 +08:00
- 变更人：assistant
- 影响范围：动态配置中心（DCC）相关代码与配置、Docker 部署文件

## 背景

原系统使用 `cn.bugstack.wrench:xfg-wrench-starter-dynamic-config-center:3.0.0` 作为动态配置中心，
底层依赖 Redis Topic（`dynamicConfigCenterRedisTopic`）广播 `AttributeVO` 消息实现配置变更下发。
本次变更将该实现替换为 Spring Cloud Alibaba Nacos Config，并使用 Docker Desktop 本地部署 Nacos Server。

## 变更内容

### 1. 新增 Nacos Docker 部署文件

- 新增 `docs/dev-ops/nacos/docker-compose.yml`：Nacos 2.2.3 standalone 模式，内嵌 derby 存储，
  暴露 8848（HTTP）、9848/9849（gRPC）端口，开启鉴权（账号 `nacos` / 密码 `nacos`），
  复用 `dev-ops_my-network` 网络。

### 2. Maven 依赖调整

- `pom.xml`（根）：
  - 在 `<dependencyManagement>` 中新增 Spring Cloud BOM `2021.0.5`、
    Spring Cloud Alibaba BOM `2021.0.5.0`（对应 Nacos 客户端 2.2.0，兼容 Nacos Server 2.2.x）。
  - 在 `<properties>` 中新增 `<lombok.version>1.18.34</lombok.version>`，
    修复 JDK 21 下原 lombok 1.18.26 与 `JCTree$JCImport.qualid` 不兼容导致编译失败的问题。
- `group-buy-market-api/pom.xml`：
  - 移除 lombok 的显式 `<version>1.18.26</version>`，改为继承父 pom 的 1.18.34。
- `grouph-buy-market-study-infrastructure/pom.xml`：
  - 移除 `xfg-wrench-starter-dynamic-config-center:3.0.0` 依赖。
  - 新增 `spring-cloud-starter-alibaba-nacos-config` 依赖（版本由 BOM 管理）。
- `grouph-buy-market-study-trigger/pom.xml`：
  - 移除 `xfg-wrench-starter-dynamic-config-center:3.0.0` 依赖。
  - 新增 `spring-cloud-starter-alibaba-nacos-config` 依赖。
  - 保留 `xfg-wrench-starter-rate-limiter`（限流组件不属于本次替换范围）。
- `grouph-buy-market-study-app/pom.xml`：
  - 新增 `spring-cloud-starter-bootstrap`，使 Spring Cloud 2021.x 加载 `bootstrap.yml`。

### 3. 配置文件调整

- 新增 `grouph-buy-market-study-app/src/main/resources/bootstrap.yml`：
  - 声明 `spring.application.name=group-buy-market-app`、`spring.profiles.active=@profileActive@`。
  - 配置 `spring.cloud.nacos.config`：server-addr、file-extension=yaml、namespace=public、group=DEFAULT_GROUP、
    username/password、refresh-enabled=true。
- `grouph-buy-market-study-app/src/main/resources/application.yml`：
  - 移除 `spring.config.name`、`spring.profiles.active`（已迁移到 bootstrap.yml）。
- `grouph-buy-market-study-app/src/main/resources/application-dev.yml`、`application-prod.yml`：
  - 移除 `xfg.wrench.config` 配置块，保留 `redis.sdk.config`（仍由 Redisson 使用）。

### 4. Java 代码调整

- `grouph-buy-market-study-infrastructure/src/main/java/com/hjs/study/infrastructure/dcc/DCCService.java`：
  - 移除 `cn.bugstack.wrench.dynamic.config.center.types.annotations.DCCValue` 注解，
    改为 `org.springframework.beans.factory.annotation.Value`，配置 key 统一加 `dcc.` 前缀，
    默认值与原 `@DCCValue` 一致（`downgradeSwitch:0`、`cutRange:100`、`scBlacklist:s02c02`、`cacheSwitch:0`）。
  - 新增 `@RefreshScope` 注解，使 Nacos 配置变更后 Bean 重建以应用新值。
  - 新增 `@Slf4j`（与全局日志风格一致）。
- `grouph-buy-market-study-trigger/src/main/java/com/hjs/study/trigger/http/DCCController.java`：
  - 移除 `RTopic dynamicConfigCenterRedisTopic` 注入与 `AttributeVO` 发布逻辑。
  - 改为注入 `com.alibaba.cloud.nacos.NacosConfigManager`，通过 `ConfigService.getConfig + publishConfig`
    拉取当前 dataId 内容（`group-buy-market-app-${profile}.yaml`），用 SnakeYAML 解析、修改 `dcc.<key>` 节点、
    重新序列化为 YAML 后整体发布。
  - 接口签名与请求路径保持不变：`GET /api/v1/gbm/dcc/update_config?key=xxx&value=xxx`。
- `group-buy-market-api/src/main/java/com/hjs/study/api/IDCCService.java`：
  - 仅更新 Javadoc，说明当前由 Nacos 承载配置变更，契约不变。

## 兼容性说明

- 接口契约不变：`IDCCService.updateConfig(String key, String value)` 签名与 `DCCController` 请求路径、参数一致。
- 业务调用方无需修改：`DCCService.isDowngradeSwitch()`、`isCutRange(userId)`、`isSCBlackIntercept(source, channel)`、
  `isCacheOpenSwitch()` 方法签名与语义保持一致。
- 配置默认值与原 `@DCCValue` 完全一致，Nacos 上未发布对应 dataId 时业务行为不变。
- 历史自定义注解 `com.hjs.study.types.annotation.DCCValue` 已在历史变更中停用，本次保留文件不删除，避免影响其他模块。

## 运行方式

1. 启动 Nacos Server：

   ```bash
   cd docs/dev-ops/nacos
   docker compose up -d
   ```

2. 访问 Nacos 控制台：`http://127.0.0.1:8848/nacos`（账号/密码：`nacos` / `nacos`）。

3. 在 Nacos 控制台 → 配置管理 → 配置列表 → `DEFAULT_GROUP` 下，建议新建 dataId：
   `group-buy-market-app-dev.yaml`，内容示例：

   ```yaml
   dcc:
     downgradeSwitch: "0"
     cutRange: "100"
     scBlacklist: s02c02
     cacheSwitch: "0"
   ```

   即使不在 Nacos 上创建该 dataId，应用启动后 `@Value` 也会使用本地默认值。

4. 启动应用后，可通过原接口动态更新配置：

   ```bash
   curl 'http://127.0.0.1:8091/api/v1/gbm/dcc/update_config?key=downgradeSwitch&value=1'
   ```

   接口会自动在 Nacos 上创建/更新 `group-buy-market-app-dev.yaml`，并通过 `@RefreshScope` 触发刷新。

## 验证结果

- `mvn -DskipTests=true clean package` 全模块构建成功（7 个模块全部 SUCCESS），
  生成 `grouph-buy-market-study-app/target/grouph-buy-market-study-app.jar`。
- 编译产物中已正确包含 `nacos-client-2.2.0.jar`、`spring-cloud-alibaba-commons-2021.0.5.0.jar`、
  `spring-cloud-context-3.1.5.jar`、`spring-cloud-starter-bootstrap-3.1.5.jar` 等关键依赖。
- Docker 部署文件 `docker-compose.yml` 已就绪，但 Docker Desktop 当前未运行，未实际拉起容器进行端到端验证。
  待 Docker Desktop 启动后执行 `docker compose up -d` 完成容器层验证。

## 未完成事项

- 待用户启动 Docker Desktop 后执行 `docker compose up -d` 拉起 Nacos 容器。
- 待 Nacos 容器启动后，进行应用与 Nacos 的端到端联通验证（拉取配置、动态更新配置触发 @RefreshScope 刷新）。
