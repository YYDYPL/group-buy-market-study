# 2-7-0628-xfg-tagNode 学习文档

## 一、分支目标

当前分支在上一阶段“拆分库表关联关系”的基础上，把人群标签能力正式接入首页拼团试算责任链。

上一阶段已经能通过 `sc_sku_activity` 找到活动配置，并且前面的人群标签分支已经能把用户写入 Redis BitSet。本分支的重点是：在试算链路中新增 `TagNode`，根据活动配置的 `tagId` 和 `tagScope` 判断当前用户是否可见、是否可参与拼团。

核心目标有三点：

1. 新增 `TagNode`，把人群标签判断接入责任链。
2. 根据活动配置计算 `visible` 和 `enable`，并写入动态上下文。
3. `EndNode` 返回真实的 `isVisible`、`isEnable`，不再写死固定值。

## 二、新增功能概览

### 1. 新增标签范围枚举

新增文件：

- `grouph-buy-market-study-domain/src/main/java/com/hjs/study/domain/activity/model/valobj/TagScopeEnumVO.java`

枚举定义了两类限制：

```java
VISIBLE(true, false, "是否可看见拼团")
ENABLE(true, false, "是否可参与拼团")
```

其中：

- `allow = true`：默认允许。
- `refuse = false`：命中限制时先拒绝。

这个枚举用于 `GroupBuyActivityDiscountVO` 中解析活动的 `tagScope`。

### 2. 活动配置增加可见/可参与判断

修改文件：

- `grouph-buy-market-study-domain/src/main/java/com/hjs/study/domain/activity/model/valobj/GroupBuyActivityDiscountVO.java`

新增方法：

```java
public boolean isVisible()
public boolean isEnable()
```

`tagScope` 的语义来自活动表字段：

- `1`：可见限制。
- `2`：参与限制。
- 多选时用分隔符组合，例如 `1,2`。

当前实现逻辑：

```text
tagScope 为空
  -> visible = true
  -> enable = true

tagScope 第一位是 1
  -> visible = false

tagScope 第二位是 2
  -> enable = false
```

也就是说，`GroupBuyActivityDiscountVO` 先根据活动配置计算“默认限制结果”。如果活动配置对可见或参与有限制，则默认拒绝，后续再由 `TagNode` 根据用户是否命中人群标签决定是否放行。

## 三、TagNode 的具体实现

新增文件：

- `grouph-buy-market-study-domain/src/main/java/com/hjs/study/domain/activity/service/trial/node/TagNode.java`

`TagNode` 继承 `AbstractGroupBuyMarketSupport`，它是试算责任树中的一个节点。

### 1. 读取活动标签配置

`TagNode` 从动态上下文读取活动配置：

```java
GroupBuyActivityDiscountVO groupBuyActivityDiscountVO = dynamicContext.getGroupBuyActivityDiscountVO();

String tagId = groupBuyActivityDiscountVO.getTagId();
boolean visible = groupBuyActivityDiscountVO.isVisible();
boolean enable = groupBuyActivityDiscountVO.isEnable();
```

其中：

- `tagId`：人群标签 ID。
- `visible`：按 `tagScope` 计算出的默认可见结果。
- `enable`：按 `tagScope` 计算出的默认可参与结果。

### 2. 无标签配置时默认放行

如果活动没有配置 `tagId`，说明不做标签限制：

```java
if (StringUtils.isBlank(tagId)) {
    dynamicContext.setVisible(true);
    dynamicContext.setEnable(true);
    return router(requestParameter, dynamicContext);
}
```

这种情况下，用户可见且可参与。

### 3. 有标签配置时查询人群命中

如果配置了 `tagId`，则通过仓储查询当前用户是否在人群范围内：

```java
boolean isWithin = repository.isTagCrowdRange(tagId, requestParameter.getUserId());
```

然后写入动态上下文：

```java
dynamicContext.setVisible(visible || isWithin);
dynamicContext.setEnable(enable || isWithin);
```

这里的含义是：

- 如果活动本身没有限制，则 `visible/enable` 已经是 `true`。
- 如果活动配置有限制，则默认是 `false`。
- 只有用户命中标签人群时，才通过 `isWithin` 放行。

### 4. 路由到 EndNode

`TagNode.get(...)` 直接返回 `EndNode`：

```java
return endNode;
```

也就是说，标签判断完成后，进入最终结果组装节点。

## 四、责任链调整

修改文件：

- `grouph-buy-market-study-domain/src/main/java/com/hjs/study/domain/activity/service/trial/node/MarketNode.java`

上一阶段正常试算路径是：

```text
MarketNode -> EndNode
```

当前分支调整为：

```text
MarketNode -> TagNode -> EndNode
```

`MarketNode` 仍然负责：

1. 异步加载活动折扣配置和商品信息。
2. 选择折扣计算策略。
3. 计算 `deductionPrice`。

当上下文完整时，`MarketNode.get(...)` 返回 `TagNode`：

```java
if (null == dynamicContext.getGroupBuyActivityDiscountVO()
        || null == dynamicContext.getSkuVO()
        || null == dynamicContext.getDeductionPrice()) {
    return errorNode;
}
return tagNode;
```

如果活动配置、商品信息或折扣价不完整，仍然进入 `ErrorNode`。

## 五、动态上下文变更

修改文件：

- `grouph-buy-market-study-domain/src/main/java/com/hjs/study/domain/activity/service/trial/factory/DefaultActivityStrategyFactory.java`

`DynamicContext` 新增两个字段：

```java
private boolean visible;
private boolean enable;
```

这两个字段由 `TagNode` 写入，由 `EndNode` 读取。

数据流如下：

```text
MarketNode
  -> 写入 groupBuyActivityDiscountVO、skuVO、deductionPrice
TagNode
  -> 写入 visible、enable
EndNode
  -> 读取所有字段并组装 TrialBalanceEntity
```

## 六、仓储层接入 Redis BitSet

### 1. 仓储接口扩展

修改文件：

- `grouph-buy-market-study-domain/src/main/java/com/hjs/study/domain/activity/adapter/repository/IActivityRepository.java`

新增方法：

```java
boolean isTagCrowdRange(String tagId, String userId);
```

领域层只关心“用户是否在人群范围内”，不关心底层是 Redis、MySQL 还是其他存储。

### 2. 基础设施实现

修改文件：

- `grouph-buy-market-study-infrastructure/src/main/java/com/hjs/study/infrastructure/adapter/repository/ActivityRepository.java`

新增注入：

```java
@Resource
private IRedisService redisService;
```

实现逻辑：

```java
RBitSet bitSet = redisService.getBitSet(tagId);
if (!bitSet.isExists()) return true;
return bitSet.get(redisService.getIndexFromUserId(userId));
```

含义：

1. 用 `tagId` 作为 Redis BitSet key。
2. 用 `userId` 计算 bitmap 下标。
3. 如果 BitSet 不存在，当前实现默认返回 `true`。
4. 如果 BitSet 存在，则读取对应 bit 位判断用户是否命中标签。

这里与前面人群标签分支打通了：`TagService` 负责把用户写入 Redis BitSet，`TagNode` 负责在试算时读取 BitSet。

## 七、结果组装变化

修改文件：

- `grouph-buy-market-study-domain/src/main/java/com/hjs/study/domain/activity/service/trial/node/EndNode.java`

`EndNode` 现在不再写死：

```java
.isVisible(false)
.isEnable(false)
```

而是从动态上下文读取：

```java
.isVisible(dynamicContext.isVisible())
.isEnable(dynamicContext.isEnable())
```

这样接口返回的 `TrialBalanceEntity` 就能体现人群标签判断结果。

## 八、测试用例变更

修改文件：

- `grouph-buy-market-study-app/src/test/java/com/hjs/study/test/domain/activity/IIndexGroupBuyMarketServiceTest.java`

当前测试包含三种场景：

### 1. 命中标签用户

```java
userId = "xiaofuge"
goodsId = "9890001"
```

该用户通常用于验证命中人群标签后的可见、可参与结果。

### 2. 未命中标签用户

新增测试：

```java
userId = "dacihua"
goodsId = "9890001"
```

用于验证用户不在人群标签范围内时，`isVisible` 和 `isEnable` 是否按活动 `tagScope` 限制返回。

### 3. 无营销配置商品

```java
goodsId = "9890002"
```

沿用上一阶段异常链路，验证没有活动配置时进入 `ErrorNode`。

## 九、完整调用链路

当前分支完整试算链路如下：

```text
IIndexGroupBuyMarketServiceImpl
  -> RootNode
  -> SwitchNode
  -> MarketNode
       -> 异步查询活动配置
       -> 异步查询商品信息
       -> 计算折扣价 deductionPrice
       -> 上下文完整则进入 TagNode
  -> TagNode
       -> 读取 tagId/tagScope
       -> 查询 Redis BitSet 判断用户是否在人群中
       -> 写入 visible/enable
  -> EndNode
       -> 组装 TrialBalanceEntity
```

正常返回中，`TrialBalanceEntity` 会包含：

- 商品信息
- 原价
- 折扣价
- 拼团目标
- 活动开始/结束时间
- 是否可见拼团
- 是否可参与拼团

## 十、当前实现边界

1. `ActivityRepository.isTagCrowdRange` 在 Redis BitSet 不存在时返回 `true`，这是一个“默认放行”的策略。
2. `TagScopeEnumVO` 当前只有 `allow/refuse` 布尔值，后续如果有更复杂的人群规则，还需要继续扩展。
3. `GroupBuyActivityDiscountVO.isEnable` 当前只有在 `tagScope` 形如 `1,2` 且第二位是 `2` 时才返回拒绝；如果只配置单独的 `2`，当前实现不会触发参与限制。
4. `TagNode` 只负责试算结果的可见/可参与判断，还没有处理真实进团时的强校验。

## 十一、代码走读顺序建议

建议按以下顺序学习：

1. `TagScopeEnumVO`：理解可见和可参与两个限制类型。
2. `GroupBuyActivityDiscountVO`：理解 `tagScope` 如何被解析成默认限制结果。
3. `IActivityRepository`：看领域层新增的人群命中查询接口。
4. `ActivityRepository.isTagCrowdRange`：看 Redis BitSet 如何判断用户是否属于标签。
5. `MarketNode`：看责任链如何从 `MarketNode` 路由到 `TagNode`。
6. `TagNode`：重点理解标签判断和上下文写入。
7. `DefaultActivityStrategyFactory.DynamicContext`：看 `visible/enable` 如何在节点间传递。
8. `EndNode`：看最终返回对象如何使用标签判断结果。
9. `IIndexGroupBuyMarketServiceTest`：对照命中、不命中、无活动配置三类用例。

## 十二、设计要点总结

1. `TagNode` 将人群标签判断独立成一个责任链节点，避免把标签逻辑混进价格计算节点。
2. `visible/enable` 通过动态上下文传递，保持节点之间低耦合。
3. 领域层只依赖 `isTagCrowdRange` 抽象能力，Redis BitSet 细节留在基础设施层。
4. 前面分支写入 Redis BitSet，本分支读取 Redis BitSet，形成了人群标签能力的闭环。
5. 最终返回结果从固定可见/可参与，升级为基于活动配置和用户标签动态计算。

