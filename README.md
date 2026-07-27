# hjs-group-buy-market-study

一个基于 Java 8、Spring Boot 2.7 和领域驱动设计（DDD）的拼团商城项目。项目覆盖数据库驱动的商品商城、运营配置后台、营销试算、交易锁单、支付结算、成团通知、逆向退款和补偿任务。

## 核心能力

- 根据用户、来源、渠道和商品执行拼团活动匹配与优惠试算。
- 支持发起新团或加入已有团队，并通过外部交易号保证锁单幂等。
- 支持支付成功后的订单结算、团队进度推进和成团判断。
- 支持未支付释放、已支付退款和成团后退款等逆向流程。
- 支持在浏览器注册和切换多个本地模拟用户，通过邀请链接完成多人参团；商品详情随机轮播两支可加入队伍，并优先展示本地用户昵称。
- 提供独立锁单待支付阶段、订单中心、团队成员详情、继续支付和退款入口。
- 通过 RabbitMQ 发布成团和退款事件，并保留通知任务补偿机制。
- 使用 Redis 实现动态配置、限流配置、分布式锁和业务缓存。
- 使用 MyBatis、MySQL 保存活动、优惠、团队订单和通知任务。
- 提供商品草稿、优惠试算、发布、下架、废弃、乐观锁和渠道切换。
- 提供管理令牌鉴权、安全图片上传、移动商城、商品详情和运营后台。
- 提供 Actuator 健康检查、Prometheus 指标和本地全流程实验台。

## 技术栈

| 类别 | 技术 |
| --- | --- |
| 运行环境 | Java 8、Maven |
| Web 框架 | Spring Boot 2.7.12 |
| 持久化 | MyBatis 2.1.4、MySQL 8.0 |
| 缓存与协调 | Redis 6.2、Redisson 3.26 |
| 消息队列 | RabbitMQ 3.12 Management |
| 可观测性 | Spring Boot Actuator、Prometheus |
| 本地前端 | HTML、CSS、原生 JavaScript |
| 部署编排 | Docker Desktop、Docker Compose |

## 业务流程

```mermaid
flowchart LR
    A["商品试算"] --> B["发起新团或加入团队"]
    B --> C["交易锁单"]
    C --> C1["待支付订单"]
    C1 --> D["确认支付并结算"]
    D --> E{"是否达到目标人数"}
    E -- "否" --> C
    E -- "是" --> F["成团通知"]
    F --> G["RabbitMQ 消费确认"]
    D --> H["申请退款"]
    H --> I["订单与团队状态回滚"]
    I --> J["退款通知"]
```

## 工程结构

```text
grouph-buy-market-study
├── group-buy-market-api                   # 对外服务接口、请求响应 DTO
├── grouph-buy-market-study-app            # Spring Boot 启动、配置、Mapper XML
├── grouph-buy-market-study-domain         # 活动、交易、结算、退款领域逻辑
├── grouph-buy-market-study-infrastructure # 仓储、DAO、Redis、MQ 和网关实现
├── grouph-buy-market-study-trigger        # HTTP、定时任务、消息监听器
├── grouph-buy-market-study-types          # 通用枚举、异常和基础类型
└── docs
    ├── changes                            # 每次项目改动记录
    ├── dev-ops                            # Docker Compose、SQL 和中间件配置
    │   └── nginx/html                     # 商城、商品详情和运营后台
    ├── documents                          # 设计与学习文档
    └── ui/group-buy-flow                  # 独立的交易链路实验台
```

模块依赖遵循“触发器调用领域服务、领域层依赖抽象、基础设施实现抽象”的方向，HTTP DTO 与领域实体保持隔离。

## 本地端口与账号

Docker Compose 中的服务只绑定本机回环地址。

| 服务 | 地址 | 账号 | 密码 |
| --- | --- | --- | --- |
| Spring Boot | `http://127.0.0.1:8091` | - | - |
| MySQL | `127.0.0.1:13306` | `root` | `123456` |
| Redis | `127.0.0.1:16379` | - | 无密码 |
| RabbitMQ AMQP | `127.0.0.1:5672` | `admin` | `admin` |
| RabbitMQ 管理台 | `http://127.0.0.1:15672` | `admin` | `admin` |
| 拼团商城 | `http://127.0.0.1:4173/` | - | - |
| 运营后台 | `http://127.0.0.1:4173/admin/login.html` | 管理令牌 | `GBM_ADMIN_TOKEN` |
| 交易实验台 | `http://127.0.0.1:4174/group-buy-flow/` | - | - |

这些账号仅用于本机开发，不应直接用于生产环境。

## 快速开始

### 1. 环境要求

- JDK 8
- Maven 3.6+
- Docker Desktop，支持 `docker compose`
- Python 3，仅用于启动静态前端服务
- IntelliJ IDEA，可选但推荐

确认版本：

```powershell
java -version
mvn -version
docker version
docker compose version
py --version
```

### 2. 启动中间件

在仓库根目录执行：

```powershell
docker compose -f docs/dev-ops/docker-compose-environment.yml up -d
```

查看状态：

```powershell
docker compose -f docs/dev-ops/docker-compose-environment.yml ps
```

Compose 会启动 MySQL、Redis、RabbitMQ 三个容器，并使用命名卷保存数据。MySQL 命名卷第一次创建时会按顺序执行：

```text
docs/dev-ops/mysql/sql/group_buy_market.sql
docs/dev-ops/mysql/sql/3-0-product-admin-and-store.sql
docs/dev-ops/mysql/sql/3-1-store-order-flow.sql
```

第二个脚本会幂等扩展商品展示与后台配置字段，并导入 8 款演示商品、优惠、活动和 `s01/c01` 映射；第三个脚本为订单中心的团队成员查询补充索引。已有 MySQL 命名卷不会自动重放初始化目录，可手动执行：

```powershell
docker exec mysql sh -lc "mysql --default-character-set=utf8mb4 -uroot -p123456 < /docker-entrypoint-initdb.d/02-product-admin-and-store.sql"
docker exec mysql sh -lc "mysql --default-character-set=utf8mb4 -uroot -p123456 < /docker-entrypoint-initdb.d/03-store-order-flow.sql"
```

### 3. 编译项目

```powershell
mvn clean -DskipTests package
```

构建产物位于：

```text
grouph-buy-market-study-app/target/grouph-buy-market-study-app.jar
```

### 4. 启动后端

#### 使用 IntelliJ IDEA

1. 将 Project SDK 设置为 JDK 8。
2. 在 Maven 面板执行 `Reload All Maven Projects`。
3. 打开 `grouph-buy-market-study-app/src/main/java/com/hjs/study/Application.java`。
4. 在 Run Configuration 的 Environment variables 中设置 `GBM_ADMIN_TOKEN`，例如 `GBM_ADMIN_TOKEN=hjs-local-admin-token`。
5. 点击 `main` 方法左侧的运行按钮。

默认配置已经启用 `dev` Profile，不需要额外填写启动参数。

#### 使用命令行

```powershell
$env:GBM_ADMIN_TOKEN = "hjs-local-admin-token"
$env:GBM_UPLOAD_DIR = "$PWD/docs/dev-ops/uploads"
java -jar grouph-buy-market-study-app/target/grouph-buy-market-study-app.jar
```

检查应用状态：

```powershell
Invoke-RestMethod http://127.0.0.1:8091/actuator/health
```

应用、MySQL、Redis、RabbitMQ 均正常时，响应状态为 `UP`。

### 5. 启动前端

在仓库根目录新开一个终端：

```powershell
py -m http.server 4173 --bind 127.0.0.1 --directory docs/dev-ops/nginx/html
```

浏览器访问：

```text
商城：http://127.0.0.1:4173/
模拟用户中心：http://127.0.0.1:4173/login.html
我的订单：http://127.0.0.1:4173/orders.html
后台：http://127.0.0.1:4173/admin/login.html
```

商城与后台默认连接本机 `8091` 后端。商城支持搜索、分类、排序、商品详情和完整拼团交易。推荐按以下顺序体验：

1. 在模拟用户中心注册用户 A、B、C。
2. 使用用户 A 打开商品详情并发起拼团，锁单后订单保持“待支付”。
3. 复制邀请链接，切换用户 B、C 后分别打开链接并加入同一个团队。
4. 在商品详情或“我的订单”中让三名用户分别确认支付，最后一人结算后成团。
5. 在订单中心验证未支付取消、成团前退款或成团后退款。

运营后台支持：

1. 使用 `GBM_ADMIN_TOKEN` 登录。
2. 维护商品资料、主图、轮播图和服务标签。
3. 配置直减、满减、N 元购或折扣策略并试算。
4. 保存草稿、发布活动、下架商品或废弃草稿。

需要集中观察 RabbitMQ 累计指标或批量实验交易参数时，可另开终端启动交易实验台：

```powershell
py -m http.server 4174 --bind 127.0.0.1 --directory docs/ui
```

## HTTP API

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| `POST` | `/api/v1/gbm/index/query_group_buy_market_config` | 商品活动与优惠试算 |
| `POST` | `/api/v1/gbm/trade/lock_market_pay_order` | 发起新团或加入团队并锁单 |
| `POST` | `/api/v1/gbm/trade/settlement_market_pay_order` | 支付成功后的订单结算 |
| `POST` | `/api/v1/gbm/trade/refund_market_pay_order` | 释放锁单或退款 |
| `GET` | `/api/v1/gbm/dcc/update_config` | 发布动态配置变更 |
| `GET` | `/api/v1/gbm/store/products` | 查询已上架商品 |
| `GET` | `/api/v1/gbm/store/products/{goodsId}` | 查询商城商品详情 |
| `GET` | `/api/v1/gbm/store/users/{userId}/orders` | 分页查询模拟用户订单 |
| `GET` | `/api/v1/gbm/store/teams/{teamId}` | 查询团队进度和成员状态 |
| `GET` | `/api/v1/gbm/admin/products` | 分页查询后台配置 |
| `POST` | `/api/v1/gbm/admin/products/trial` | 优惠策略试算 |
| `POST` | `/api/v1/gbm/admin/products/draft` | 保存组合配置草稿 |
| `POST` | `/api/v1/gbm/admin/products/{goodsId}/publish` | 发布草稿 |
| `POST` | `/api/v1/gbm/admin/products/{goodsId}/offline` | 下架商品 |
| `POST` | `/api/v1/gbm/admin/images` | 上传商品图片 |
| `GET` | `/actuator/health` | 应用与中间件健康检查 |

`/api/v1/gbm/admin/**` 必须携带请求头 `X-Admin-Token`。令牌只保存在后台页面的 `sessionStorage`，服务端未配置令牌时管理接口默认关闭。

统一业务响应结构：

```json
{
  "code": "0000",
  "info": "成功",
  "data": {}
}
```

### 商品试算

```bash
curl -X POST "http://127.0.0.1:8091/api/v1/gbm/index/query_group_buy_market_config" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "hjs01",
    "source": "s01",
    "channel": "c01",
    "goodsId": "9890001"
  }'
```

本地初始化数据中，商品 `9890001` 对应活动 `100123`，默认试算价格为原价 `100.00`、优惠 `20.00`、支付价 `80.00`。

### 发起新团并锁单

`teamId` 为空表示发起新团。加入已有团队时，将它替换为首次锁单返回的团队 ID。

```bash
curl -X POST "http://127.0.0.1:8091/api/v1/gbm/trade/lock_market_pay_order" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "hjs01",
    "teamId": null,
    "activityId": 100123,
    "goodsId": "9890001",
    "source": "s01",
    "channel": "c01",
    "outTradeNo": "202607240001",
    "notifyConfigVO": {
      "notifyType": "MQ",
      "notifyMQ": "topic.team_success"
    }
  }'
```

### 支付结算

```bash
curl -X POST "http://127.0.0.1:8091/api/v1/gbm/trade/settlement_market_pay_order" \
  -H "Content-Type: application/json" \
  -d '{
    "source": "s01",
    "channel": "c01",
    "userId": "hjs01",
    "outTradeNo": "202607240001",
    "outTradeTime": "2026-07-24T10:00:00+08:00"
  }'
```

### 退款

```bash
curl -X POST "http://127.0.0.1:8091/api/v1/gbm/trade/refund_market_pay_order" \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "hjs01",
    "outTradeNo": "202607240001",
    "source": "s01",
    "channel": "c01"
  }'
```

### 动态配置

```bash
curl "http://127.0.0.1:8091/api/v1/gbm/dcc/update_config?key=downgradeSwitch&value=0"
curl "http://127.0.0.1:8091/api/v1/gbm/dcc/update_config?key=cutRange&value=100"
curl "http://127.0.0.1:8091/api/v1/gbm/dcc/update_config?key=rateLimiterSwitch&value=open"
```

## 环境变量

`application-dev.yml` 提供本机默认值，也支持环境变量覆盖：

| 环境变量 | 默认值 | 用途 |
| --- | --- | --- |
| `MYSQL_HOST` | `127.0.0.1` | MySQL 主机 |
| `MYSQL_PORT` | `13306` | MySQL 端口 |
| `MYSQL_DATABASE` | `group_buy_market` | 数据库名称 |
| `MYSQL_USERNAME` | `root` | MySQL 用户名 |
| `MYSQL_PASSWORD` | `123456` | MySQL 密码 |
| `REDIS_HOST` | `127.0.0.1` | Redis 主机 |
| `REDIS_PORT` | `16379` | Redis 端口 |
| `RABBITMQ_HOST` | `127.0.0.1` | RabbitMQ 主机 |
| `RABBITMQ_PORT` | `5672` | RabbitMQ AMQP 端口 |
| `RABBITMQ_USERNAME` | `admin` | RabbitMQ 用户名 |
| `RABBITMQ_PASSWORD` | `admin` | RabbitMQ 密码 |
| `GBM_ADMIN_TOKEN` | 空 | 运营后台管理令牌；为空时后台接口关闭 |
| `GBM_UPLOAD_DIR` | `./uploads` | 商品图片上传目录 |

PowerShell 示例：

```powershell
$env:MYSQL_PASSWORD = "new-password"
$env:RABBITMQ_PASSWORD = "new-password"
$env:GBM_ADMIN_TOKEN = "hjs-local-admin-token"
$env:GBM_UPLOAD_DIR = "$PWD/docs/dev-ops/uploads"
java -jar grouph-buy-market-study-app/target/grouph-buy-market-study-app.jar
```

## RabbitMQ 拓扑

| 类型 | 名称 |
| --- | --- |
| Topic Exchange | `group_buy_market_exchange` |
| 成团路由键 | `topic.team_success` |
| 成团队列 | `group_buy_market_queue_2_topic_team_success` |
| 退款路由键 | `topic.team_refund` |
| 退款队列 | `group_buy_market_queue_2_topic_team_refund` |

管理台中 `Ready=0`、`Unacked=0`、`Total=0` 通常表示消息已经被在线消费者立即处理并确认，并不表示队列没有配置。前端实验台展示的是累计 `publish`、`ack` 和当前 `messages`，更适合观察本地快速消费链路。

## 验证命令

```powershell
# 校验 Compose
docker compose -f docs/dev-ops/docker-compose-environment.yml config

# 编译全部 Maven 模块
mvn clean -DskipTests package

# 检查商城与后台 JavaScript 语法
node --check docs/dev-ops/nginx/html/js/store-api.js
node --check docs/dev-ops/nginx/html/js/store-identity.js
node --check docs/dev-ops/nginx/html/js/index.js
node --check docs/dev-ops/nginx/html/js/login.js
node --check docs/dev-ops/nginx/html/js/product-detail.js
node --check docs/dev-ops/nginx/html/js/orders.js
node --check docs/dev-ops/nginx/html/admin/js/admin.js

# 检查交易实验台 JavaScript 语法
node --check docs/ui/group-buy-flow/app.js

# 检查应用健康状态
Invoke-RestMethod http://127.0.0.1:8091/actuator/health
```

## 停止服务

在 IDEA 中启动的后端，使用 Run 窗口的红色停止按钮关闭。

按端口停止命令行启动的后端：

```powershell
Get-NetTCPConnection -LocalPort 8091 -State Listen |
  ForEach-Object { Stop-Process -Id $_.OwningProcess }
```

前端使用 `py -m http.server` 启动时，在对应终端按 `Ctrl+C`。也可以按端口停止：

```powershell
Get-NetTCPConnection -LocalPort 4173 -State Listen |
  ForEach-Object { Stop-Process -Id $_.OwningProcess }
```

停止 Docker 中间件但保留容器和命名卷：

```powershell
docker compose -f docs/dev-ops/docker-compose-environment.yml stop
```

## 常见问题

### 端口已被占用

```powershell
Get-NetTCPConnection -LocalPort 8091,4173,4174,13306,16379,5672,15672 -ErrorAction SilentlyContinue |
  Select-Object LocalPort, State, OwningProcess
```

确认占用进程后再决定是否停止，避免误关其他项目的共享中间件。

### 首页试算被限流

首页接口按用户 ID 限流。短时间内重复点击时，可以等待一秒后重试，或在前端重置本轮测试以生成新的测试用户。

### RabbitMQ 队列看不到消息

应用启动后会自动声明交换机、路由和两个业务队列。消费者在线时，消息会快速完成投递和确认，管理台列表中的当前积压会保持为零。累计 `publish` 和 `ack` 可在 RabbitMQ 管理台或独立交易实验台中查看。

### 数据库没有初始化

初始化 SQL 只在 MySQL 命名卷首次创建时自动执行。先检查 `group_buy_market` 数据库是否存在，以及当前 Compose 是否复用了历史命名卷；已有卷可手动重复执行幂等升级脚本。不要为了重新导入业务库而直接删除包含其他项目数据的共享卷。

### 前端无法调用后端

依次确认：

1. `http://127.0.0.1:8091/actuator/health` 是否为 `UP`。
2. 前端连接配置中的应用地址是否为 `http://127.0.0.1:8091`。
3. 浏览器开发者工具 Network 面板中是否存在接口错误。
4. 当前测试用户是否触发了限流或业务次数限制。

## 文档与改动记录

- `docs/documents/`：业务设计和学习文档。
- `docs/dev-ops/`：本地与部署环境配置。
- `docs/changes/`：按日期保存的真实改动记录。
- `AGENT.md`：项目改动记录规约。

每次独立修改都必须在 `docs/changes/` 新增中文主题文档，文件名以 `_MMDD.md` 结尾，并在正文写明精确变更时间、变更内容和验证结果。
