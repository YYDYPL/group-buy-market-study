# 2-6-0628-xfg-split-table 学习文档

## 一、分支目标

当前分支在上一阶段“人群标签数据采集”的基础上，对拼团活动查询链路做了拆表改造。

这次的“split table”并不是引入分库分表中间件，而是把原本活动表里承载的“渠道、来源、商品与活动关系”拆到独立关联表 `sc_sku_activity` 中。这样 `group_buy_activity` 更专注于活动本身，`sc_sku_activity` 专门维护不同渠道、来源、商品对应哪个拼团活动。

核心目标有三点：

1. 新增渠道商品活动配置关联表 `sc_sku_activity`。
2. 将首页试算链路改为先查商品活动映射，再按 `activityId` 查询活动与折扣配置。
3. 新增无营销配置的异常节点，让不存在活动配置的商品走清晰的错误分支。

## 二、为什么要拆表

上一阶段活动查询主要依赖 `source`、`channel` 直接查询 `group_buy_activity`。随着活动模型变复杂，活动主表如果继续保存渠道、来源、商品等映射字段，会让活动本身和投放关系耦合在一起。

本分支拆分后：

- `group_buy_activity`：保存活动本体，如活动 ID、活动名称、折扣 ID、拼团目标、状态、时间、人群标签等。
- `sc_sku_activity`：保存渠道、来源、商品与活动的映射关系。

查询链路从：

```text
source + channel -> group_buy_activity
```

变为：

```text
source + channel + goodsId
  -> sc_sku_activity
  -> activityId
  -> group_buy_activity
  -> group_buy_discount
```

这样做后，一个活动可以被不同渠道商品映射引用，活动配置和商品投放关系也更容易独立维护。

## 三、数据库脚本变更

相关文件：

- `docs/dev-ops/mysql/sql/group_buy_market.sql`
- `docs/dev-ops/mysql/sql-bak/2-6-group_buy_market.sql`

### 1. 新增 `sc_sku_activity`

新增表：

```sql
CREATE TABLE `sc_sku_activity` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增ID',
  `source` varchar(8) NOT NULL COMMENT '渠道',
  `channel` varchar(8) NOT NULL COMMENT '来源',
  `activity_id` bigint(8) NOT NULL COMMENT '活动ID',
  `goods_id` varchar(16) NOT NULL COMMENT '商品ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uq_sc_goodsid` (`source`,`channel`,`goods_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='渠道商品活动配置关联表';
```

测试数据：

```sql
(1,'s01','c01',100123,'9890001',...)
```

含义是：`s01 + c01 + 9890001` 这个渠道商品，对应活动 `100123`。

### 2. 调整 `group_buy_activity`

`group_buy_activity` 表现在更聚焦活动本身，不再在 PO 中承载 `source`、`channel`、`goodsId`。活动查询新增按 `activityId` 查询有效活动的方式。

当前 SQL 样例里，活动 `100123` 的状态为 `1`，表示生效：

```sql
(1,100123,'测试活动','25120208',0,1,1,15,1,...)
```

## 四、领域层变更

### 1. 仓储接口改造

修改文件：

- `grouph-buy-market-study-domain/src/main/java/com/hjs/study/domain/activity/adapter/repository/IActivityRepository.java`

接口现在包含三类查询：

```java
GroupBuyActivityDiscountVO queryGroupBuyActivityDiscountVO(Long activityId);

SkuVO querySkuByGoodsId(String goodsId);

SCSkuActivityVO querySCSkuActivityBySCGoodsId(String source, String channel, String goodsId);
```

关键变化是：查询活动折扣配置不再直接传 `source`、`channel`，而是传 `activityId`。

### 2. 新增 `SCSkuActivityVO`

新增文件：

- `grouph-buy-market-study-domain/src/main/java/com/hjs/study/domain/activity/model/valobj/SCSkuActivityVO.java`

该值对象用于承载渠道商品活动映射：

- `source`：渠道
- `chanel`：来源
- `activityId`：活动 ID
- `goodsId`：商品 ID

领域层通过这个 VO 获取 `activityId`，再继续查询活动配置。

## 五、试算链路变更

### 1. 活动配置异步任务改造

修改文件：

- `grouph-buy-market-study-domain/src/main/java/com/hjs/study/domain/activity/service/trial/thread/QueryGroupBuyActivityDiscountVOThreadTask.java`

该任务现在接收：

```java
source, channel, goodsId, activityRepository
```

执行流程：

```java
SCSkuActivityVO scSkuActivityVO =
        activityRepository.querySCSkuActivityBySCGoodsId(source, channel, goodsId);

if (null == scSkuActivityVO) return null;

return activityRepository.queryGroupBuyActivityDiscountVO(scSkuActivityVO.getActivityId());
```

也就是说，活动配置查询被拆成两步：

1. 通过渠道、来源、商品 ID 查 `sc_sku_activity`。
2. 通过映射得到的 `activityId` 查活动与折扣配置。

### 2. `MarketNode` 增加异常分支

修改文件：

- `grouph-buy-market-study-domain/src/main/java/com/hjs/study/domain/activity/service/trial/node/MarketNode.java`
- `grouph-buy-market-study-domain/src/main/java/com/hjs/study/domain/activity/service/trial/node/ErrorNode.java`

`MarketNode` 仍然并行加载两类数据：

- 活动折扣配置
- 商品信息

但现在活动配置可能因为没有 `sc_sku_activity` 映射而返回 `null`。因此 `MarketNode.doApply` 增加了空值判断：

```java
GroupBuyActivityDiscountVO groupBuyActivityDiscountVO = dynamicContext.getGroupBuyActivityDiscountVO();
if (null == groupBuyActivityDiscountVO) {
    return router(requestParameter, dynamicContext);
}
```

`get` 方法也根据上下文完整性决定下一个节点：

```java
if (null == dynamicContext.getGroupBuyActivityDiscountVO()
        || null == dynamicContext.getSkuVO()
        || null == dynamicContext.getDeductionPrice()) {
    return errorNode;
}
return endNode;
```

如果活动配置、商品信息或折扣计算结果不完整，就走 `ErrorNode`。

### 3. 新增 `ErrorNode`

新增文件：

- `grouph-buy-market-study-domain/src/main/java/com/hjs/study/domain/activity/service/trial/node/ErrorNode.java`

`ErrorNode` 用于处理无营销配置场景：

```java
if (null == dynamicContext.getGroupBuyActivityDiscountVO() || null == dynamicContext.getSkuVO()) {
    throw new AppException(ResponseCode.E0002.getCode(), ResponseCode.E0002.getInfo());
}
```

这让“商品没有拼团活动配置”的情况不再继续进入正常结果组装，而是明确抛出业务异常。

## 六、基础设施层实现

### 1. 仓储实现改造

修改文件：

- `grouph-buy-market-study-infrastructure/src/main/java/com/hjs/study/infrastructure/adapter/repository/ActivityRepository.java`

新增注入：

```java
@Resource
private ISCSkuActivityDao skuActivityDao;
```

新增方法：

```java
public SCSkuActivityVO querySCSkuActivityBySCGoodsId(String source, String channel, String goodsId)
```

该方法组装 `SCSkuActivity` 查询对象，调用 DAO 查询映射，再转换为领域层 `SCSkuActivityVO`。

活动配置查询也调整为按 `activityId` 查询：

```java
GroupBuyActivity groupBuyActivityRes =
        groupBuyActivityDao.queryValidGroupBuyActivityId(activityId);
```

再通过 `discountId` 查询折扣配置，最终组装 `GroupBuyActivityDiscountVO`。

### 2. 新增 DAO 和 PO

新增文件：

- `grouph-buy-market-study-infrastructure/src/main/java/com/hjs/study/infrastructure/dao/ISCSkuActivityDao.java`
- `grouph-buy-market-study-infrastructure/src/main/java/com/hjs/study/infrastructure/dao/po/SCSkuActivity.java`

`SCSkuActivity` 是 `sc_sku_activity` 表对应的 PO，字段包括：

- `source`
- `channel`
- `activityId`
- `goodsId`
- `createTime`
- `updateTime`

`ISCSkuActivityDao` 提供映射查询：

```java
SCSkuActivity querySCSkuActivityBySCGoodsId(SCSkuActivity scSkuActivity);
```

### 3. Mapper 变更

新增文件：

- `grouph-buy-market-study-app/src/main/resources/mybatis/mapper/sc_sku_activity_mapper.xml`

新增查询：

```sql
select source, channel, activity_id, goods_id
from sc_sku_activity
where goods_id = #{goodsId}
```

当前 Java 层会传入 `source`、`channel`、`goodsId`，但 Mapper 当前只按 `goods_id` 查询。表结构已经有 `(source, channel, goods_id)` 唯一索引，后续如果要严格按渠道和来源隔离活动，可以把 SQL 补成：

```sql
where source = #{source}
  and channel = #{channel}
  and goods_id = #{goodsId}
```

修改文件：

- `grouph-buy-market-study-app/src/main/resources/mybatis/mapper/group_buy_activity_mapper.xml`

新增按活动 ID 查询有效活动：

```sql
select activity_id, activity_name, discount_id, group_type, take_limit_count,
       target, valid_time, status, start_time, end_time, tag_id, tag_scope
from group_buy_activity
where activity_id = #{activityId} and status = 1
```

## 七、异常码变更

修改文件：

- `grouph-buy-market-study-types/src/main/java/com/hjs/study/types/enums/ResponseCode.java`

新增错误码：

```java
E0002("E0002", "无拼团营销配置")
```

当商品没有活动映射或活动配置不完整时，`ErrorNode` 会抛出该异常。

## 八、测试用例变更

修改文件：

- `grouph-buy-market-study-app/src/test/java/com/hjs/study/test/domain/activity/IIndexGroupBuyMarketServiceTest.java`

保留正常试算用例：

```java
goodsId = "9890001"
```

该商品在 `sc_sku_activity` 中有映射，能查询到活动 `100123` 并继续完成折扣试算。

新增异常测试用例：

```java
goodsId = "9890002"
```

该商品没有活动映射，活动配置查询返回 `null`，最终进入 `ErrorNode`，抛出“无拼团营销配置”业务异常。

## 九、完整调用链路

当前分支首页试算的主链路如下：

```text
IIndexGroupBuyMarketServiceImpl
  -> RootNode
  -> SwitchNode
  -> MarketNode
       -> 异步查询商品信息 sku
       -> 异步查询活动配置
            -> sc_sku_activity
            -> group_buy_activity
            -> group_buy_discount
       -> 折扣策略计算 deductionPrice
       -> 根据上下文完整性路由
            -> EndNode
            -> ErrorNode
```

正常路径：

```text
source/channel/goodsId -> sc_sku_activity -> activityId -> group_buy_activity -> discount -> EndNode
```

异常路径：

```text
source/channel/goodsId -> 无 sc_sku_activity 映射 -> ErrorNode -> E0002
```

## 十、代码走读顺序建议

建议按以下顺序学习：

1. `docs/dev-ops/mysql/sql/group_buy_market.sql`：先看 `sc_sku_activity` 和 `group_buy_activity` 的表结构变化。
2. `SCSkuActivityVO`：理解领域层如何承载映射关系。
3. `IActivityRepository`：看仓储接口如何从 source/channel 查询切换为 activityId 查询。
4. `QueryGroupBuyActivityDiscountVOThreadTask`：看活动配置异步查询的新两段式流程。
5. `ActivityRepository`：看 PO 到 VO 的转换和活动/折扣聚合。
6. `sc_sku_activity_mapper.xml` 与 `group_buy_activity_mapper.xml`：看具体 SQL。
7. `MarketNode`：看空配置如何进入异常分支。
8. `ErrorNode`：看无营销配置异常如何抛出。
9. `IIndexGroupBuyMarketServiceTest`：对照正常商品和异常商品两个用例。

## 十一、设计要点总结

1. 活动主表和渠道商品映射表拆开，降低活动配置与投放关系的耦合。
2. 活动查询链路由 `source/channel` 直接查活动，改为先查 `sc_sku_activity` 得到 `activityId`。
3. 领域层新增 `SCSkuActivityVO`，让映射关系以业务语义进入领域。
4. `MarketNode` 增加空配置判断，不完整上下文会进入 `ErrorNode`。
5. `ErrorNode` 和 `E0002` 让“无拼团营销配置”成为清晰的业务异常。
6. 当前 Mapper 已有分表映射查询，但实际 SQL 只按 `goods_id` 过滤，后续可补齐 `source`、`channel` 条件以更贴合唯一索引。

