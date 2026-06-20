# 2-3-0620-xfg-multi-thread 学习文档

## 一、分支目标

当前分支在上一阶段“试算模型抽象模板设计”的基础上，为首页拼团商品试算流程增加了多线程数据加载能力，并补齐了活动、折扣、商品三类数据的仓储查询链路。

核心目标有三点：

1. 在策略路由框架中增加“执行节点业务前先并行加载数据”的扩展点。
2. 在试算流程中并行查询拼团活动折扣配置和商品信息，减少串行数据库查询带来的等待时间。
3. 补齐领域仓储接口、基础设施仓储实现、DAO、MyBatis Mapper、SKU 表结构和单元测试，让 `indexMarketTrial` 可以跑通完整的数据查询和结果组装链路。

## 二、新增功能概览

### 1. 多线程策略路由模板

新增文件：

- `grouph-buy-market-study-types/src/main/java/com/hjs/study/types/design/framework/tree/AbstractMultiThreadStrategyRouter.java`

该抽象类继承原有策略树能力：

- `StrategyMapper<T, D, R>`：负责根据当前请求和动态上下文选择下一个策略节点。
- `StrategyHandler<T, D, R>`：负责执行当前策略节点。

它在 `apply` 中固定了一个模板流程：

```java
public R apply(T requestParameter, D dynamicContext) throws Exception {
    multiThread(requestParameter, dynamicContext);
    return doApply(requestParameter, dynamicContext);
}
```

也就是说，每个节点被执行时会先调用 `multiThread`，再调用 `doApply`。

- `multiThread(...)`：给节点预留异步加载数据的扩展点。
- `doApply(...)`：承载节点原本的业务处理逻辑。
- `router(...)`：根据 `get(...)` 找到下一个节点并继续执行。

这个设计让“异步数据准备”和“业务流程处理”分开，每个节点可以按需决定是否并行加载数据。

### 2. 首页拼团试算入口服务

新增和修改文件：

- `grouph-buy-market-study-domain/src/main/java/com/hjs/study/domain/activity/service/IIndexGroupBuyMarketService.java`
- `grouph-buy-market-study-domain/src/main/java/com/hjs/study/domain/activity/service/IIndexGroupBuyMarketServiceImpl.java`

服务接口新增 `indexMarketTrial(MarketProductEntity marketProductEntity)`，用于首页拼团商品试算。

实现类通过 `DefaultActivityStrategyFactory` 获取策略处理器：

```java
StrategyHandler<MarketProductEntity, DynamicContext, TrialBalanceEntity> strategyHandler =
        defaultActivityStrategyFactory.strategyHandler();

return strategyHandler.apply(marketProductEntity, new DynamicContext());
```

这里的关键点是：入口服务不关心试算内部有哪些节点，也不关心数据如何加载，只负责创建动态上下文并启动策略链。

### 3. 拼团试算责任链

本分支将试算流程组织为以下节点：

```text
IIndexGroupBuyMarketServiceImpl
    -> RootNode
    -> SwitchNode
    -> MarketNode
    -> EndNode
```

相关文件：

- `grouph-buy-market-study-domain/src/main/java/com/hjs/study/domain/activity/service/trial/node/RootNode.java`
- `grouph-buy-market-study-domain/src/main/java/com/hjs/study/domain/activity/service/trial/node/SwitchNode.java`
- `grouph-buy-market-study-domain/src/main/java/com/hjs/study/domain/activity/service/trial/node/MarketNode.java`
- `grouph-buy-market-study-domain/src/main/java/com/hjs/study/domain/activity/service/trial/node/EndNode.java`

各节点职责如下：

| 节点 | 主要职责 |
| --- | --- |
| `RootNode` | 打印入口日志，校验 `userId`、`goodsId`、`source`、`channel` 等必要参数，然后路由到 `SwitchNode`。 |
| `SwitchNode` | 当前分支中主要作为流程开关节点，继续路由到 `MarketNode`，后续可以扩展不同营销试算分支。 |
| `MarketNode` | 并行加载活动折扣配置和商品信息，将结果写入动态上下文，再继续路由到 `EndNode`。 |
| `EndNode` | 从动态上下文读取活动和商品数据，组装最终 `TrialBalanceEntity` 返回。 |

### 4. 动态上下文承载试算中间数据

修改文件：

- `grouph-buy-market-study-domain/src/main/java/com/hjs/study/domain/activity/service/trial/factory/DefaultActivityStrategyFactory.java`

`DefaultActivityStrategyFactory.DynamicContext` 新增两个字段：

```java
private GroupBuyActivityDiscountVO groupBuyActivityDiscountVO;
private SkuVO skuVO;
```

这两个对象分别表示：

- `GroupBuyActivityDiscountVO`：拼团活动和折扣配置聚合数据。
- `SkuVO`：商品基础信息。

动态上下文的作用是把节点之间共享的数据集中保存起来。`MarketNode` 负责写入，`EndNode` 负责读取并组装返回结果。

## 三、多线程加载的具体实现

### 1. 公共支持类

修改文件：

- `grouph-buy-market-study-domain/src/main/java/com/hjs/study/domain/activity/service/trial/AbstractGroupBuyMarketSupport.java`

该类继承 `AbstractMultiThreadStrategyRouter`，是拼团试算领域节点的公共父类。

它统一注入：

```java
protected IActivityRepository repository;
```

并提供默认空实现：

```java
protected void multiThread(...) {
    // 默认方法
}
```

这样不是所有节点都必须实现异步加载逻辑。比如 `RootNode`、`SwitchNode`、`EndNode` 当前不需要额外预加载数据，可以直接使用默认实现；`MarketNode` 需要并行查询数据，所以单独重写 `multiThread`。

### 2. MarketNode 并行查询两类数据

核心文件：

- `grouph-buy-market-study-domain/src/main/java/com/hjs/study/domain/activity/service/trial/node/MarketNode.java`

`MarketNode` 注入线程池：

```java
@Resource
private ThreadPoolExecutor threadPoolExecutor;
```

并在 `multiThread` 中创建两个 `FutureTask`：

```java
FutureTask<GroupBuyActivityDiscountVO> groupBuyActivityDiscountVOFutureTask =
        new FutureTask<>(queryGroupBuyActivityDiscountVOThreadTask);

FutureTask<SkuVO> skuVOFutureTask =
        new FutureTask<>(querySkuVOFromDBThreadTask);
```

两个任务分别执行：

- 根据 `source`、`channel` 查询有效拼团活动和对应折扣配置。
- 根据 `goodsId` 查询商品信息。

然后交给线程池执行：

```java
threadPoolExecutor.execute(groupBuyActivityDiscountVOFutureTask);
threadPoolExecutor.execute(skuVOFutureTask);
```

最后通过 `get(timeout, TimeUnit.MINUTES)` 等待结果并写入动态上下文：

```java
dynamicContext.setGroupBuyActivityDiscountVO(groupBuyActivityDiscountVOFutureTask.get(timeout, TimeUnit.MINUTES));
dynamicContext.setSkuVO(skuVOFutureTask.get(timeout, TimeUnit.MINUTES));
```

这样活动折扣查询和商品查询可以同时进行，节点后续处理只需要从上下文中读取结果。

### 3. Callable 任务封装

新增文件：

- `grouph-buy-market-study-domain/src/main/java/com/hjs/study/domain/activity/service/trial/thread/QueryGroupBuyActivityDiscountVOThreadTask.java`
- `grouph-buy-market-study-domain/src/main/java/com/hjs/study/domain/activity/service/trial/thread/QuerySkuVOFromDBThreadTask.java`

两个任务都实现 `Callable`：

```java
public class QuerySkuVOFromDBThreadTask implements Callable<SkuVO> {
    @Override
    public SkuVO call() {
        return activityRepository.querySkuByGoodsId(goodsId);
    }
}
```

任务类只保留查询所需参数和仓储接口，不直接依赖 MyBatis、DAO 或数据库实现。这保持了领域层对基础设施层的隔离。

### 4. 线程池配置

相关文件：

- `grouph-buy-market-study-app/src/main/java/com/hjs/study/config/ThreadPoolConfig.java`
- `grouph-buy-market-study-app/src/main/java/com/hjs/study/config/ThreadPoolConfigProperties.java`
- `grouph-buy-market-study-app/src/main/resources/application-dev.yml`

项目通过 `thread.pool.executor.config` 配置线程池参数：

```yaml
thread:
  pool:
    executor:
      config:
        core-pool-size: 20
        max-pool-size: 50
        keep-alive-time: 5000
        block-queue-size: 5000
        policy: CallerRunsPolicy
```

`ThreadPoolConfig` 根据配置创建 `ThreadPoolExecutor`，并根据 `policy` 选择拒绝策略。当前配置使用 `CallerRunsPolicy`，当线程池和队列无法继续接收任务时，由调用线程执行任务，避免任务被直接丢弃。

## 四、仓储与数据查询实现

### 1. 领域仓储接口

新增文件：

- `grouph-buy-market-study-domain/src/main/java/com/hjs/study/domain/activity/adapter/repository/IActivityRepository.java`

接口定义了领域需要的数据能力：

```java
GroupBuyActivityDiscountVO queryGroupBuyActivityDiscountVO(String source, String channel);

SkuVO querySkuByGoodsId(String goodsId);
```

领域服务和线程任务只依赖这个接口，不直接依赖数据库表结构。

### 2. 基础设施仓储实现

新增文件：

- `grouph-buy-market-study-infrastructure/src/main/java/com/hjs/study/infrastructure/adapter/repository/ActivityRepository.java`

`ActivityRepository` 实现 `IActivityRepository`，负责聚合 DAO 查询结果并转换为领域 VO。

`queryGroupBuyActivityDiscountVO` 的流程：

1. 根据 `source`、`channel` 查询最新有效拼团活动。
2. 从活动中获取 `discountId`。
3. 根据 `discountId` 查询折扣配置。
4. 将活动信息和折扣信息组装为 `GroupBuyActivityDiscountVO`。

`querySkuByGoodsId` 的流程：

1. 根据 `goodsId` 查询 `sku` 表。
2. 将基础设施 PO `Sku` 转换为领域对象 `SkuVO`。

### 3. DAO 和 Mapper

新增和修改文件：

- `grouph-buy-market-study-infrastructure/src/main/java/com/hjs/study/infrastructure/dao/IGroupBuyActivityDao.java`
- `grouph-buy-market-study-infrastructure/src/main/java/com/hjs/study/infrastructure/dao/IGroupBuyDiscountDao.java`
- `grouph-buy-market-study-infrastructure/src/main/java/com/hjs/study/infrastructure/dao/ISkuDao.java`
- `grouph-buy-market-study-app/src/main/resources/mybatis/mapper/group_buy_activity_mapper.xml`
- `grouph-buy-market-study-app/src/main/resources/mybatis/mapper/group_buy_discount_mapper.xml`
- `grouph-buy-market-study-app/src/main/resources/mybatis/mapper/sku_mapper.xml`

新增查询包括：

- `queryValidGroupBuyActivity`：根据 `source`、`channel` 查询最新活动。
- `queryGroupBuyActivityDiscountByDiscountId`：根据 `discountId` 查询折扣配置。
- `querySkuByGoodsId`：根据 `goodsId` 查询商品信息。

### 4. 新增商品数据模型

新增文件：

- `grouph-buy-market-study-infrastructure/src/main/java/com/hjs/study/infrastructure/dao/po/Sku.java`
- `grouph-buy-market-study-domain/src/main/java/com/hjs/study/domain/activity/model/valobj/SkuVO.java`

`Sku` 是基础设施层 PO，对应数据库 `sku` 表。

`SkuVO` 是领域层值对象，只暴露试算流程需要的字段：

```java
private String goodsId;
private String goodsName;
private BigDecimal originalPrice;
```

### 5. 活动折扣聚合值对象

新增文件：

- `grouph-buy-market-study-domain/src/main/java/com/hjs/study/domain/activity/model/valobj/GroupBuyActivityDiscountVO.java`

该对象将活动和折扣配置合并在一起：

- 外层保存活动维度数据，比如 `activityId`、`source`、`channel`、`goodsId`、`target`、`startTime`、`endTime`。
- 内部静态类 `GroupBuyDiscount` 保存折扣维度数据，比如 `discountName`、`discountType`、`marketPlan`、`marketExpr`。

这样 `EndNode` 或后续营销计算节点无需关心数据来自几张表，只需要读取领域语义清晰的聚合对象。

## 五、数据库脚本变更

相关文件：

- `docs/dev-ops/mysql/sql/group_buy_market.sql`
- `docs/dev-ops/mysql/sql-bak/2-2-group_buy_market.sql`
- `docs/dev-ops/mysql/sql-bak/2-3-group_buy_market.sql`

当前分支新增了 `sku` 表和一条测试商品数据：

```sql
CREATE TABLE `sku` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `source` varchar(8) NOT NULL COMMENT '渠道',
  `channel` varchar(8) NOT NULL COMMENT '来源',
  `goods_id` varchar(16) NOT NULL COMMENT '商品ID',
  `goods_name` varchar(128) NOT NULL COMMENT '商品名称',
  `original_price` decimal(10,2) NOT NULL COMMENT '商品价格',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品信息';
```

测试数据中 `goods_id` 为 `9890001`，与单元测试请求保持一致。

## 六、最终返回结果组装

核心文件：

- `grouph-buy-market-study-domain/src/main/java/com/hjs/study/domain/activity/service/trial/node/EndNode.java`

`EndNode` 从上下文中取出：

```java
GroupBuyActivityDiscountVO groupBuyActivityDiscountVO = dynamicContext.getGroupBuyActivityDiscountVO();
SkuVO skuVO = dynamicContext.getSkuVO();
```

然后构建 `TrialBalanceEntity`：

```java
return TrialBalanceEntity.builder()
        .goodsId(skuVO.getGoodsId())
        .goodsName(skuVO.getGoodsName())
        .originalPrice(skuVO.getOriginalPrice())
        .deductionPrice(new BigDecimal("0.00"))
        .targetCount(groupBuyActivityDiscountVO.getTarget())
        .startTime(groupBuyActivityDiscountVO.getStartTime())
        .endTime(groupBuyActivityDiscountVO.getEndTime())
        .isVisible(false)
        .isEnable(false)
        .build();
```

当前分支还没有真正实现“优惠金额试算”，所以 `deductionPrice` 固定为 `0.00`，`isVisible` 和 `isEnable` 固定为 `false`。不过数据准备链路已经完成，后续可以在 `MarketNode` 或新增节点中补充具体营销规则计算。

## 七、测试入口

新增文件：

- `grouph-buy-market-study-app/src/test/java/com/hjs/study/test/domain/activity/IIndexGroupBuyMarketServiceTest.java`

测试用例构造请求：

```java
MarketProductEntity marketProductEntity = new MarketProductEntity();
marketProductEntity.setUserId("xiaofuge");
marketProductEntity.setSource("s01");
marketProductEntity.setChannel("c01");
marketProductEntity.setGoodsId("9890001");
```

调用：

```java
TrialBalanceEntity trialBalanceEntity =
        indexGroupBuyMarketService.indexMarketTrial(marketProductEntity);
```

这条测试链路会覆盖：

1. 首页试算入口服务。
2. 试算策略链路由。
3. `MarketNode` 多线程数据加载。
4. 仓储聚合查询。
5. MyBatis Mapper 查询。
6. `EndNode` 返回对象组装。

## 八、代码走读顺序建议

建议按以下顺序学习当前分支：

1. 先读 `AbstractMultiThreadStrategyRouter`，理解模板方法如何把 `multiThread` 放到 `doApply` 之前。
2. 再读 `IIndexGroupBuyMarketServiceImpl`，看入口如何启动策略链。
3. 顺着 `DefaultActivityStrategyFactory` 找到 `RootNode`。
4. 按 `RootNode -> SwitchNode -> MarketNode -> EndNode` 的顺序走读节点。
5. 重点看 `MarketNode.multiThread`，理解两个 `FutureTask` 如何并行执行并写入上下文。
6. 继续读两个 `Callable` 任务，确认它们只依赖领域仓储接口。
7. 读 `IActivityRepository` 和 `ActivityRepository`，理解领域层和基础设施层的分工。
8. 最后读 Mapper、SQL 和测试用例，把数据库数据与返回结果对应起来。

## 九、设计要点总结

1. 模板方法扩展点：`AbstractMultiThreadStrategyRouter` 把“异步加载数据”沉淀为所有节点可复用的标准步骤。
2. 责任链组织流程：各节点只处理自己的职责，再通过 `router` 交给下一个节点。
3. 动态上下文传递数据：避免节点之间通过复杂参数互相传递中间结果。
4. 领域依赖抽象仓储：线程任务和领域节点依赖 `IActivityRepository`，不直接感知 DAO 和 SQL。
5. 基础设施负责数据适配：`ActivityRepository` 负责多表查询和 PO 到 VO 的转换。
6. 并发优化点集中：当前只有 `MarketNode` 重写 `multiThread`，其他节点保持默认空实现，扩展点清晰且影响范围可控。

