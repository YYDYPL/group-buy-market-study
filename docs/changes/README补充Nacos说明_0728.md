# README 补充 Nacos 配置中心说明

## 变更信息

- 变更时间：2026-07-28 09:45:00 +08:00
- 变更人：assistant
- 影响范围：README.md 文档

## 背景

DCC 动态配置中心已从 xfg-wrench + Redis Topic 迁移至 Nacos（详见 `docs/changes/DCC动态配置中心迁移至Nacos_0728.md`），但 README 仍按旧实现描述，需要同步更新，让本地启动、配置项、环境变量与现状一致。

## 变更内容

对 `README.md` 做以下 9 处更新：

1. **核心能力**：原"使用 Redis 实现动态配置、限流配置、分布式锁和业务缓存"拆分为两行——动态配置由 Nacos 承载，Redis 仅保留限流、分布式锁和缓存。
2. **技术栈表**：新增"配置中心"行，列出 Nacos 2.2.3、Spring Cloud Alibaba 2021.0.5.0、Spring Cloud 2021.0.5。
3. **本地端口与账号表**：新增 Nacos 控制台（`http://127.0.0.1:8848/nacos`）和 Nacos Open API 两行，账号 `nacos/nacos`，并补充 Open API 鉴权说明。
4. **启动中间件**：新增"启动 Nacos 配置中心"小节，给出独立 compose 启动命令与 `group-buy-market-app-dev.yaml` 初始内容示例。
5. **动态配置章节**：重写为基于 Nacos 的 DCC 说明，列出 4 个配置项及默认值，更新 curl 示例（移除历史错误的 `rateLimiterSwitch`，改为 `scBlacklist` 与 `cacheSwitch`），并说明完整刷新链路。
6. **环境变量表**：新增 `NACOS_HOST`、`NACOS_PORT`、`NACOS_NAMESPACE`、`NACOS_GROUP`、`NACOS_USERNAME`、`NACOS_PASSWORD` 6 个变量，并标注在 `bootstrap.yml` 中读取、修改后需重启。
7. **停止服务**：新增 `docker compose -f docs/dev-ops/nacos/docker-compose.yml stop` 命令。
8. **常见问题**：端口占用排查列表新增 `8848/9848/9849`；新增"Nacos 配置拉取失败"排查项，给出 4 步定位方法。
9. **验证命令**：新增 Nacos 健康检查与 DCC 端到端验证命令。

## 验证结果

- 通过 `Read` 工具确认 9 处编辑均生效，README 整体结构未破坏。
- 文档示例命令与 `docs/dev-ops/nacos/docker-compose.yml`、`bootstrap.yml`、`DCCController` 实际行为一致。
- 已通过端到端测试验证：`curl 'http://127.0.0.1:8091/api/v1/gbm/dcc/update_config?key=downgradeSwitch&value=1'` 返回 `{"code":"0000","info":"成功"}`，Nacos 控制台对应 dataId 内容已更新。

## 兼容性说明

- 仅文档变更，不涉及代码与配置，不影响运行时行为。
- 旧的 `rateLimiterSwitch` 示例本就与 `DCCService` 实际配置项不符（限流由 `xfg-wrench-starter-rate-limiter` 独立管理，不在 DCC 范围内），本次一并修正。
