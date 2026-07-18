# 2-9-0628-xfg-trade 学习文档

## 一、分支目标

当前分支在上一阶段"DCC 动态配置中心"的基础上，新增拼团交易锁单功能模块，打通从商品试算到交易锁单的完整业务闭环。

本分支的核心目标是：用户在下单支付前，先通过营销锁单预占拼团优惠资格。锁单成功后才能进入真正的支付环节，后续会有两个分支流程——支付成功（消费完成）和超时未支付（回退锁单数量）。

## 二、新增功能概览

本分支新增一整套交易模块，按 DDD 分层架构划分如下：

| 分层 | 模块 | 说明 |
|------|------|------|
| API | `group-buy-market-api` | 交易服务接口与 DTO 定义 |
| Trigger | `grouph-buy-market-study-trigger` | HTTP 控制器，实现 API 接口 |
| Domain | `grouph-buy-market-study-domain` | 交易领域服务、聚合、实体、值对象 |
| Infrastructure | `grouph-buy-market-study-infrastructure` | 仓储实现、DAO、PO |
| App | `grouph-buy-market-study-app` | MyBatis Mapper XML |
| Types | `grouph-buy-market-study-types` | 错误码枚举扩展 |

### 新增文件清单（共 20 个新文件 + 2 个 mapper XML）

**API 层：**
- `group-buy-market-api/.../IMarketTradeService.java`
- `group-buy-market-api/.../dto/LockMarketPayOrderRequestDTO.java`
- `group-buy-market-api/.../dto/LockMarketPayOrderResponseDTO.java`

**Controller 层：**
- `grouph-buy-market-study-trigger/.../http/MarketTradeController.java`

**Domain 层：**
- `.../domain/trade/adapter/repository/ITradeRepository.java`
- `.../domain/trade/model/aggregate/GroupBuyOrderAggregate.java`
- `.../domain/trade/model/entity/MarketPayOrderEntity.java`
- `.../domain/trade/model/entity/PayActivityEntity.java`
- `.../domain/trade/model/entity/PayDiscountEntity.java`
- `.../domain/trade/model/entity/UserEntity.java`
- `.../domain/trade/model/valobj/GroupBuyProgressVO.java`
- `.../domain/trade/model/valobj/TradeOrderStatusEnumVO.java`
- `.../domain/trade/service/ITradeOrderService.java`
- `.../domain/trade/service/TradeOrderService.java`

**Infrastructure 层：**
- `.../infrastructure/adapter/repository/TradeRepository.java`
- `.../infrastructure/dao/IGroupBuyOrderDao.java`
- `.../infrastructure/dao/IGroupBuyOrderListDao.java`
- `.../infrastructure/dao/po/GroupBuyOrder.java`
- `.../infrastructure/dao/po/GroupBuyOrderList.java`

**Mapper XML：**
- `group_buy_order_mapper.xml`
- `group_buy_order_list_mapper.xml`

**测试：**
- `.../test/domain/trade/ITradeOrderServiceTest.java`
- `.../test/domain/trigger/ITradeOrderServiceTest.java`

## 三、数据库表结构

本分支新使用以下两张核心表（已存在于 `group_buy_market` 库中）：

### 1. group_buy_order（拼团主表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | int(11) | 自增ID |
| team_id | varchar(8) | 拼单组队ID（唯一） |
| activity_id | bigint(8) | 活动ID |
| target_count | int(5) | 拼团目标数量 |
| complete_count | int(5) | 已完成数量 |
| lock_count | int(5) | 锁单数量（当前已锁） |
| source/channel | varchar(8) | 渠道/来源 |
| original_price | decimal(8,2) | 原始价格 |
| deduction_price | decimal(8,2) | 折扣金额 |
| pay_price | decimal(8,2) | 支付价格 |
| status | tinyint(1) | 状态（0拼单中、1完成、2失败） |

`lock_count` 是本分支核心列——每次锁单 +1，通过 SQL `where lock_count < target_count` 保证不超拼。

### 2. group_buy_order_list（拼团订单明细表）

| 字段 | 类型 | 说明 |
|------|------|------|
| id | int(11) | 自增ID |
| user_id | varchar(64) | 用户ID |
| team_id | varchar(8) | 所属拼单组队ID |
| order_id | varchar(12) | 订单ID（唯一） |
| activity_id | bigint(8) | 活动ID |
| goods_id | varchar(16) | 商品ID |
| start_time/end_time | datetime | 活动开始/结束时间 |
| original_price | decimal(8,2) | 原始价格 |
| deduction_price | decimal(8,2) | 折扣金额 |
| status | tinyint(1) | 状态（0初始锁定、1消费完成、2超时关单） |
| out_trade_no | varchar(12) | 外部交易单号（幂等键） |

`out_trade_no + user_id + status=0` 是幂等查询的核心组合条件。

## 四、核心业务流程

### 锁单完整链路

```
HTTP Request
  -> MarketTradeController.lockMarketPayOrder()
    -> 参数校验（userId、source、channel、goodsId、activityId 必填）
    -> tradeOrderService.queryNoPayMarketPayOrderByOutTradeNo()  [幂等检查]
    -> 如果已存在锁单记录，直接返回已有结果
    -> 如果传了 teamId，tradeOrderService.queryGroupBuyProgress()  [拼团进度检查]
    -> 如果 lockCount >= targetCount，返回 E0006（拼团已满）
    -> indexGroupBuyMarketService.indexMarketTrial()  [营销优惠试算]
    -> tradeOrderService.lockMarketPayOrder()  [执行锁单]
      -> TradeOrderService 构建 GroupBuyOrderAggregate
      -> TradeRepository.lockMarketPayOrder()  [事务性锁单]
        -> 如果 teamId 为空：INSERT group_buy_order（开新团）
        -> 如果 teamId 不为空：UPDATE group_buy_order SET lock_count = lock_count + 1
           （where team_id = ? and lock_count < target_count）
           -> 更新影响行数 != 1，则抛出 E0005（拼团已满）
        -> INSERT group_buy_order_list（写入订单明细，含 out_trade_no 幂等键）
        -> 返回 MarketPayOrderEntity（orderId、deductionPrice、status）
    -> 组装 LockMarketPayOrderResponseDTO 返回
```

### 关键设计点

**1. 幂等性保障（双重机制）**

- **应用层幂等**：Controller 先查 `out_trade_no + userId` 是否已有未支付订单，有则直接返回。
- **数据库层幂等**：`group_buy_order_list` 的 `uq_order_id` 唯一索引 + `out_trade_no` 字段组合。如果并发绕过应用层检查，`DuplicateKeyException` 会被捕获并转译为 `E0005`。

**2. 拼团满员保护**

- **新团**：首次锁单时 `lock_count = 1，target_count` 从试算结果获取。
- **加入已有团**：SQL `update ... set lock_count = lock_count + 1 where team_id = ? and lock_count < target_count`，利用 MySQL 行锁 + WHERE 条件保证不会超拼。

**3. 团进度预检**

在锁单前，如果上游传了 `teamId`，先查询 `target_count` 和 `lock_count`，若已满则直接返回 `E0006`，避免无效的试算调用。

## 五、API 接口定义

### IMarketTradeService

```java
public interface IMarketTradeService {
    Response<LockMarketPayOrderResponseDTO> lockMarketPayOrder(
        LockMarketPayOrderRequestDTO request);
}
```

### LockMarketPayOrderRequestDTO

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| userId | String | 是 | 用户ID |
| teamId | String | 否 | 拼单组队ID，为空则创建新团 |
| activityId | Long | 是 | 活动ID |
| goodsId | String | 是 | 商品ID |
| source | String | 是 | 渠道 |
| channel | String | 是 | 来源 |
| outTradeNo | String | 是 | 外部交易单号（幂等键） |

### LockMarketPayOrderResponseDTO

| 字段 | 类型 | 说明 |
|------|------|------|
| orderId | String | 预购订单ID（12位数字） |
| deductionPrice | BigDecimal | 折扣金额 |
| tradeOrderStatus | Integer | 交易订单状态（0创建、1完成、2关单） |

### HTTP 访问路径

```text
POST /api/v1/gbm/trade/lockMarketPayOrder
```

## 六、Domain 层设计

### 1. 聚合根：GroupBuyOrderAggregate

```java
public class GroupBuyOrderAggregate {
    private UserEntity userEntity;              // 用户实体
    private PayActivityEntity payActivityEntity; // 支付活动实体
    private PayDiscountEntity payDiscountEntity; // 支付优惠实体
}
```

聚合将用户、活动、优惠三个维度的实体组装在一起，作为锁单操作的整体入参传递给仓储层。

### 2. 实体对象

**UserEntity** — 用户实体

```java
private String userId;
```

**PayActivityEntity** — 支付活动实体

```java
private String teamId;        // 拼单组队ID（空则新团）
private Long activityId;      // 活动ID
private String activityName;  // 活动名称
private Date startTime;       // 活动开始时间
private Date endTime;         // 活动结束时间
private Integer targetCount;  // 拼团目标数量
```

**PayDiscountEntity** — 支付优惠实体

```java
private String source;           // 渠道
private String channel;          // 来源
private String goodsId;          // 商品ID
private String goodsName;        // 商品名称
private BigDecimal originalPrice; // 原始价格
private BigDecimal deductionPrice;// 折扣金额
private String outTradeNo;       // 外部交易单号
```

**MarketPayOrderEntity** — 锁单结果实体

```java
private String orderId;                          // 订单ID
private BigDecimal deductionPrice;                // 折扣金额
private TradeOrderStatusEnumVO tradeOrderStatusEnumVO; // 订单状态
```

### 3. 值对象

**TradeOrderStatusEnumVO** — 订单状态枚举

| 状态 | code | 说明 |
|------|------|------|
| CREATE | 0 | 初始创建（锁单成功） |
| COMPLETE | 1 | 消费完成（支付成功） |
| CLOSE | 2 | 超时关单（回退锁单） |

注意：当前分支只用到 `CREATE`。`COMPLETE` 和 `CLOSE` 是预留的支付成功和超时回退状态。

**GroupBuyProgressVO** — 拼团进度值对象

```java
private Integer targetCount;   // 目标数量
private Integer completeCount; // 完成数量
private Integer lockCount;     // 锁单数量
```

### 4. 领域服务：ITradeOrderService / TradeOrderService

```java
public interface ITradeOrderService {
    // 查询未支付订单（幂等）
    MarketPayOrderEntity queryNoPayMarketPayOrderByOutTradeNo(
        String userId, String outTradeNo);

    // 查询拼团进度
    GroupBuyProgressVO queryGroupBuyProgress(String teamId);

    // 锁单
    MarketPayOrderEntity lockMarketPayOrder(
        UserEntity userEntity,
        PayActivityEntity payActivityEntity,
        PayDiscountEntity payDiscountEntity);
}
```

`TradeOrderService.lockMarketPayOrder()` 的核心职责是将三个实体组装成 `GroupBuyOrderAggregate`，然后委托给仓储执行锁单。

## 七、Infrastructure 层实现

### TradeRepository（仓储实现）

这是本分支最核心的实现类，包含三个方法：

**1. queryMarketPayOrderEntityByOutTradeNo**

查询幂等：根据 `userId + outTradeNo + status=0` 查询 `group_buy_order_list`，若存在则返回已有的订单信息。

**2. lockMarketPayOrder（@Transactional）**

核心锁单逻辑，放在一个事务中：

```
1. 判断 teamId 是否为空:
   - 空: 生成 8 位随机数作为新 teamId，INSERT group_buy_order
         （lock_count=1, complete_count=0, target_count 来自试算）
   - 不空: UPDATE group_buy_order SET lock_count = lock_count + 1
           WHERE team_id = ? AND lock_count < target_count
           影响行数 != 1 -> 抛 E0005

2. 生成 12 位随机数作为 orderId
3. INSERT group_buy_order_list（写入订单明细）
   - 捕获 DuplicateKeyException -> 抛 E0005（并发幂等保护）

4. 返回 MarketPayOrderEntity
```

**3. queryGroupBuyProgress**

查询 `group_buy_order` 中的 `target_count`、`complete_count`、`lock_count`，用于拼团进度预检。

### DAO 接口

**IGroupBuyOrderDao**

```java
void insert(GroupBuyOrder groupBuyOrder);
int updateAddLockCount(String teamId);        // lock_count + 1
int updateSubtractionLockCount(String teamId); // lock_count - 1（预留回退）
GroupBuyOrder queryGroupBuyProgress(String teamId);
```

**IGroupBuyOrderListDao**

```java
void insert(GroupBuyOrderList groupBuyOrderListReq);
GroupBuyOrderList queryGroupBuyOrderRecordByOutTradeNo(GroupBuyOrderList req);
```

### MyBatis SQL 要点

**updateAddLockCount（关键 SQL）**

```xml
<update id="updateAddLockCount" parameterType="java.lang.String">
    <![CDATA[
        update group_buy_order
        set lock_count = lock_count + 1, update_time = now()
        where team_id = #{teamId} and lock_count < target_count
    ]]>
</update>
```

`lock_count < target_count` 条件确保在数据库层面不会超拼。依赖 MySQL 行锁机制，并发安全。

## 八、Controller 层处理流程

`MarketTradeController` 是 API 接口的 HTTP 实现，完整处理流程：

**步骤 1：参数校验**

```java
if (StringUtils.isBlank(userId) || StringUtils.isBlank(source)
    || StringUtils.isBlank(channel) || StringUtils.isBlank(goodsId)
    || null == activityId) {
    return Response.builder()
        .code(ResponseCode.ILLEGAL_PARAMETER.getCode())
        .info(ResponseCode.ILLEGAL_PARAMETER.getInfo())
        .build();
}
```

**步骤 2：幂等检查**

查询 `outTradeNo` 是否已有未支付订单，有则直接返回已有结果（状态码 `SUCCESS`，data 为已有订单信息）。

**步骤 3：拼团进度预检**

如果传了 `teamId`，查询 `target_count` 和 `lock_count` 是否相等。若相等，表示拼团已满，返回 `E0006`。

**步骤 4：营销优惠试算**

调用 `indexGroupBuyMarketService.indexMarketTrial()` 获取 `GroupBuyActivityDiscountVO`（活动信息）和 `TrialBalanceEntity`（商品信息、价格、折扣）。

**步骤 5：执行锁单**

将试算结果组装为 `UserEntity`、`PayActivityEntity`、`PayDiscountEntity`，调用 `tradeOrderService.lockMarketPayOrder()`。

**步骤 6：返回结果**

组装 `LockMarketPayOrderResponseDTO` 返回。

**异常处理：**
- `AppException`：返回对应业务错误码和信息
- `Exception`：返回 `UN_ERROR`

## 九、错误码扩展

修改文件：`grouph-buy-market-study-types/.../enums/ResponseCode.java`

新增：

```java
E0005("E0005", "拼团组队失败，记录更新为0")
E0006("E0006", "拼团组队完结，锁单量已达成")
```

还有 `AppException` 新增的构造方法，支持通过 `ResponseCode` 直接创建异常：

```java
public AppException(ResponseCode responseCode) {
    this.code = responseCode.getCode();
    this.info = responseCode.getInfo();
}
```

触发场景：
- **E0005**：加入已有团时，`updateAddLockCount` 影响行数为 0（拼团已满），或 `insert group_buy_order_list` 遇到唯一键冲突。
- **E0006**：锁单前预检发现 `lockCount >= targetCount`，提前拦截。

## 十、已有文件的修改

除了新增交易模块，本分支还修改了以下已有文件：

| 文件 | 修改内容 |
|------|----------|
| `MarketProductEntity.java` | 新增 `activityId` 字段，支持直接指定活动ID |
| `TrialBalanceEntity.java` | 新增 `groupBuyActivityDiscountVO` 字段，试算结果带回活动信息 |
| `IIndexGroupBuyMarketServiceImpl.java` | 配合 `MarketProductEntity.activityId` 调整 |
| `EndNode.java` | 试算结果增加 `groupBuyActivityDiscountVO` 返回 |
| `MarketNode.java` | 配合 entity 字段变更 |
| `QueryGroupBuyActivityDiscountVOThreadTask.java` | 返回活动折扣信息 |
| `group_buy_market.sql` | 主 SQL 脚本同步更新 |

核心变更点：试算链路的 `TrialBalanceEntity` 现在会携带 `GroupBuyActivityDiscountVO`（活动名称、起止时间、拼团目标），使交易锁单模块可以直接从试算结果中拿到所需的全部活动信息。

## 十一、测试用例

新增文件：`ITradeOrderServiceTest.java`（domain 和 trigger 两个包下各一个）

测试流程：

```java
// 1. 入参
Long activityId = 100123L;
String userId = "xiaofuge";
String goodsId = "9890001";
String source = "s01";
String channel = "c01";
String outTradeNo = "909000098111";

// 2. 营销优惠试算
TrialBalanceEntity trialBalanceEntity = indexGroupBuyMarketService
    .indexMarketTrial(MarketProductEntity.builder()...build());

// 3. 幂等检查
MarketPayOrderEntity old = tradeOrderService
    .queryNoPayMarketPayOrderByOutTradeNo(userId, outTradeNo);
if (old != null) return; // 已有记录则跳过

// 4. 执行锁单（teamId=null，开新团）
MarketPayOrderEntity newOrder = tradeOrderService.lockMarketPayOrder(
    UserEntity.builder().userId(userId).build(),
    PayActivityEntity.builder()
        .teamId(null)  // 开新团
        .activityId(...)
        .targetCount(...)
        .build(),
    PayDiscountEntity.builder()
        .source(source).channel(channel).goodsId(goodsId)
        .originalPrice(...).deductionPrice(...)
        .outTradeNo(outTradeNo)
        .build()
);
```

## 十二、当前实现边界

1. **支付成功回调未实现**：当前只有锁单（`CREATE` 状态），`COMPLETE`（消费完成）和 `CLOSE`（超时关单/回退锁单）是两个后续分支，本分支未覆盖。
2. **超时回退未实现**：锁单后如果超时未支付，需要回退 `lock_count`（`updateSubtractionLockCount` 已在 DAO 中定义但未接入业务逻辑）。
3. **teamId 生成**：当前使用 `RandomStringUtils.randomNumeric(8)`，生产环境应替换为雪花算法 UUID 或更可靠的分布式 ID 方案。orderId 同理。
4. **拼团完成判断**：当前 `lock_count == target_count` 即为满团，但 `complete_count` 字段尚未在锁单时更新（需要支付成功后更新）。
5. **空团查询优化**：如果 `teamId` 为空，不需要查询拼团进度，Controller 已做此优化。
6. **事务范围**：锁单事务超时 500ms（`@Transactional(timeout = 500)`），长事务风险较低，但需要注意 MySQL 行锁持有时间。

## 十三、代码走读顺序建议

建议按以下顺序学习：

1. `LockMarketPayOrderRequestDTO` / `LockMarketPayOrderResponseDTO`：理解接口入参和出参。
2. `IMarketTradeService`：理解 API 接口定义。
3. `MarketTradeController`：理解 HTTP 层完整处理流程（重点阅读幂等、预检、试算、锁单四步）。
4. `ITradeOrderService` / `TradeOrderService`：理解领域服务如何组装聚合。
5. `GroupBuyOrderAggregate`：理解聚合对象结构。
6. `MarketPayOrderEntity` / `PayActivityEntity` / `PayDiscountEntity` / `UserEntity`：理解各实体职责。
7. `TradeOrderStatusEnumVO` / `GroupBuyProgressVO`：理解值对象定义。
8. `ITradeRepository`：理解仓储接口抽象。
9. `TradeRepository.lockMarketPayOrder()`：重点阅读锁单核心实现（新团/老团分支、事务边界、并发保护）。
10. `IGroupBuyOrderDao` / `IGroupBuyOrderListDao`：理解数据访问接口。
11. `GroupBuyOrder` / `GroupBuyOrderList`：理解 PO 映射。
12. `group_buy_order_mapper.xml` / `group_buy_order_list_mapper.xml`：理解 SQL 实现（尤其是 `updateAddLockCount` 的并发安全写法）。
13. `ITradeOrderServiceTest`：跑通完整测试流程。

## 十四、设计要点总结

1. **幂等设计双层保障**：应用层查 `outTradeNo` + 数据库层唯一索引 `uq_order_id` + `DuplicateKeyException` 捕获，确保同一外部单号不会重复锁单。
2. **并发安全通过 SQL 实现**：`update group_buy_order set lock_count = lock_count + 1 where team_id = ? and lock_count < target_count`，利用 MySQL 行锁 + WHERE 条件在数据库层面保证不会超拼，无需应用层加锁。
3. **聚合模式隔离复杂对象**：`GroupBuyOrderAggregate` 将用户、活动、优惠三个实体打包，使领域服务只需关注业务逻辑，仓储只需关注持久化。
4. **DDD 分层清晰**：API 接口（`IMarketTradeService`）-> Controller（`MarketTradeController`）-> Domain Service（`TradeOrderService`）-> Repository（`ITradeRepository`/`TradeRepository`）-> DAO（MyBatis），每层职责明确。
5. **状态机预留**：`TradeOrderStatusEnumVO` 已定义 CREATE/COMPLETE/CLOSE 三种状态，为后续支付成功和超时回退打好基础。
6. **试算与交易解耦**：`indexMarketTrial` 是已有的试算能力，交易模块通过 `TrialBalanceEntity` 和 `GroupBuyActivityDiscountVO` 获取所需的活动和价格信息，不直接依赖试算内部实现。
