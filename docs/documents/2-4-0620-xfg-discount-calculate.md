# 2-4-0620-xfg-discount-calculate 学习文档

## 一、分支目标

当前分支是在上一阶段“多线程异步数据加载”基础上，继续把拼团试算中的“优惠金额计算”落成策略模式。

本分支主要完成了三件事：

1. 把折扣类型从单一逻辑扩展为可插拔的策略实现。
2. 让 `MarketNode` 根据活动配置里的 `marketPlan` 动态选择折扣计算服务。
3. 补齐数据库测试数据，使直减、满减、折扣、N 元购四种优惠都能被演示和验证。

## 二、新增功能概览

### 1. 折扣类型枚举

新增文件：

- `grouph-buy-market-study-domain/src/main/java/com/hjs/study/domain/activity/model/valobj/DiscountTypeEnum.java`

该枚举目前定义了两类折扣类型：

- `BASE(0, "基础优惠")`
- `TAG(1, "人群标签")`

`GroupBuyActivityDiscountVO.GroupBuyDiscount` 中的 `discountType` 由原来的数值字段调整为枚举类型，仓储层会把数据库中的 `discount_type` 转成枚举。

这样做的好处是：

- 业务代码不用直接比较魔法数字。
- 后续增加新的折扣类型时，领域语义更清晰。

### 2. 折扣计算策略接口

新增文件：

- `grouph-buy-market-study-domain/src/main/java/com/hjs/study/domain/activity/service/discount/IDiscountCalculateService.java`

接口定义统一的折扣计算方法：

```java
BigDecimal calculate(String userId, BigDecimal originalPrice, GroupBuyActivityDiscountVO.GroupBuyDiscount groupBuyDiscount);
```

参数含义：

- `userId`：当前用户，用于人群标签校验。
- `originalPrice`：商品原价。
- `groupBuyDiscount`：活动折扣配置。

### 3. 折扣计算抽象模板

新增文件：

- `grouph-buy-market-study-domain/src/main/java/com/hjs/study/domain/activity/service/discount/AbstractDiscountCalculateService.java`

这是本分支的核心抽象类。

它把通用逻辑和差异逻辑拆开：

1. 如果折扣类型是 `TAG`，先做标签过滤。
2. 标签校验通过后，再交给子类执行具体计算。

```java
public BigDecimal calculate(...) {
    if (DiscountTypeEnum.TAG.equals(groupBuyDiscount.getDiscountType())) {
        boolean isCrowdRange = filterTagId(userId, groupBuyDiscount.getTagId());
        if (!isCrowdRange) return originalPrice;
    }
    return doCalculate(originalPrice, groupBuyDiscount);
}
```

目前 `filterTagId` 还是预留实现，返回 `true`，说明标签人群控制在这一版里先保留结构，后续再补规则。

### 4. 四种营销计划的策略实现

新增文件：

- `grouph-buy-market-study-domain/src/main/java/com/hjs/study/domain/activity/service/discount/impl/ZJCalculateService.java`
- `grouph-buy-market-study-domain/src/main/java/com/hjs/study/domain/activity/service/discount/impl/MJCalculateService.java`
- `grouph-buy-market-study-domain/src/main/java/com/hjs/study/domain/activity/service/discount/impl/ZKCalculateService.java`
- `grouph-buy-market-study-domain/src/main/java/com/hjs/study/domain/activity/service/discount/impl/NCalculateService.java`

四个类都继承 `AbstractDiscountCalculateService`，并通过 `@Service("...")` 注册成不同名字的 Spring Bean。

对应关系如下：

| Bean 名称 | 营销计划 | 含义 |
| --- | --- | --- |
| `ZJ` | 直减 | 直接减固定金额 |
| `MJ` | 满减 | 满足门槛后减固定金额 |
| `ZK` | 折扣 | 按折扣系数计算 |
| `N` | N 元购 | 直接按指定价格购买 |

#### 直减 `ZJ`

计算方式：

```java
deductionPrice = originalPrice - marketExpr
```

如果结果小于等于 0，则最低返回 `0.01`。

#### 满减 `MJ`

计算方式：

1. 从 `marketExpr` 中拆出门槛金额和减免金额，例如 `100,10`。
2. 如果原价小于门槛，则不打折。
3. 否则按 `原价 - 减免金额` 计算。
4. 最低支付金额仍然不能低于 `0.01`。

#### 折扣 `ZK`

计算方式：

```java
deductionPrice = originalPrice * marketExpr
```

例如 `0.8` 就表示 8 折。

#### N 元购 `N`

计算方式最直接：

```java
return new BigDecimal(marketExpr);
```

也就是把优惠价直接作为结果价。

## 三、试算链路如何接入折扣计算

### 1. `MarketNode` 注入折扣策略集合

修改文件：

- `grouph-buy-market-study-domain/src/main/java/com/hjs/study/domain/activity/service/trial/node/MarketNode.java`

`MarketNode` 现在多了一个策略映射表：

```java
@Resource
private Map<String, IDiscountCalculateService> discountCalculateServiceMap;
```

Spring 会把所有实现了 `IDiscountCalculateService` 且带有 Bean 名称的实现类注入进来，键就是 Bean 名。

### 2. 根据 `marketPlan` 动态选择策略

在 `doApply` 里，`MarketNode` 先从动态上下文拿到：

- `GroupBuyActivityDiscountVO`
- `SkuVO`

再读取折扣配置里的 `marketPlan`：

```java
IDiscountCalculateService discountCalculateService =
        discountCalculateServiceMap.get(groupBuyDiscount.getMarketPlan());
```

如果没找到对应实现，就抛出业务异常：

```java
throw new AppException(ResponseCode.E0001.getCode(), ResponseCode.E0001.getInfo());
```

这一步把“找策略”和“算价格”分开了，新增一种营销计划时，只需要新增一个实现类，不需要改主流程。

### 3. 计算折扣价并写入上下文

折扣价计算完成后写入动态上下文：

```java
BigDecimal deductionPrice = discountCalculateService.calculate(
        requestParameter.getUserId(),
        skuVO.getOriginalPrice(),
        groupBuyDiscount);

dynamicContext.setDeductionPrice(deductionPrice);
```

然后继续路由到 `EndNode`。

### 4. `EndNode` 使用折扣价组装返回结果

修改文件：

- `grouph-buy-market-study-domain/src/main/java/com/hjs/study/domain/activity/service/trial/node/EndNode.java`

`EndNode` 现在不再写死 `deductionPrice`，而是直接从上下文读取：

```java
BigDecimal deductionPrice = dynamicContext.getDeductionPrice();
```

最终返回的 `TrialBalanceEntity` 就包含了真实的试算结果。

## 四、仓储层和数据模型调整

### 1. 活动折扣 VO 调整

修改文件：

- `grouph-buy-market-study-domain/src/main/java/com/hjs/study/domain/activity/model/valobj/GroupBuyActivityDiscountVO.java`

`GroupBuyDiscount.discountType` 从原始数值改成了 `DiscountTypeEnum`：

```java
private DiscountTypeEnum discountType;
```

这让 `AbstractDiscountCalculateService` 可以直接按枚举判断标签类优惠，而不是再去比较整型值。

### 2. 仓储适配枚举转换

修改文件：

- `grouph-buy-market-study-infrastructure/src/main/java/com/hjs/study/infrastructure/adapter/repository/ActivityRepository.java`

基础设施层在组装 `GroupBuyDiscount` 时做了枚举转换：

```java
discountType(DiscountTypeEnum.get(groupBuyDiscountRes.getDiscountType()))
```

也就是说，数据库里仍然保存整数，但进入领域层时已经变成了领域枚举。

### 3. 折扣 PO 调整

修改文件：

- `grouph-buy-market-study-infrastructure/src/main/java/com/hjs/study/infrastructure/dao/po/GroupBuyDiscount.java`

`marketPlan` 的含义被扩展为支持：

- `ZJ` 直减
- `MJ` 满减
- `ZK` 折扣
- `N` 元购

这也是策略分发的核心字段。

### 4. 异常码扩展

修改文件：

- `grouph-buy-market-study-types/src/main/java/com/hjs/study/types/enums/ResponseCode.java`

新增：

```java
E0001("E0001", "不存在对应的折扣计算服务")
```

当 `marketPlan` 找不到对应 Bean 时，就用这个错误码返回。

## 五、数据库脚本变更

相关文件：

- `docs/dev-ops/mysql/sql/group_buy_market.sql`
- `docs/dev-ops/mysql/sql-bak/2-4-group_buy_market.sql`

这次分支更新了测试数据，重点是 `group_buy_discount` 表新增了多种营销计划样本：

```sql
(1,'25120207','直减优惠20元',...,'ZJ','20',...)
(2,'25120208','满减优惠100-10元',...,'MJ','100,10',...)
(4,'25120209','折扣优惠8折',...,'ZK','0.8',...)
(5,'25120210','N元购买优惠',...,'N','1.99',...)
```

同时 `sku` 表继续提供测试商品：

- `goods_id = 9890001`
- `original_price = 100.00`

这些数据正好能覆盖四种折扣策略的验证。

## 六、完整调用链路

当前分支的试算流程可以按下面理解：

```text
IIndexGroupBuyMarketServiceImpl
  -> RootNode
  -> SwitchNode
  -> MarketNode
       -> 并行查活动配置
       -> 并行查商品信息
       -> 根据 marketPlan 选择折扣策略
       -> 计算 deductionPrice
  -> EndNode
  -> TrialBalanceEntity
```

其中最关键的变化是：

- 上一版只做到“并行加载数据”
- 这一版进一步做到“按活动配置动态选择折扣算法并输出最终试算价”

## 七、代码走读顺序建议

建议按这个顺序看：

1. `DiscountTypeEnum`
2. `IDiscountCalculateService`
3. `AbstractDiscountCalculateService`
4. `ZJCalculateService`、`MJCalculateService`、`ZKCalculateService`、`NCalculateService`
5. `MarketNode`
6. `EndNode`
7. `ActivityRepository`
8. `group_buy_market.sql`

这样最容易把“数据库配置 -> 仓储转换 -> 策略选择 -> 结果计算”串起来。

## 八、设计要点总结

1. 策略模式把不同优惠算法拆成独立实现，新增营销计划时更容易扩展。
2. 抽象模板把公共的标签校验逻辑前置，子类只管具体算法。
3. `Map<String, IDiscountCalculateService>` 让 Spring Bean 名直接充当策略路由键。
4. 领域层用枚举和 VO 表达业务语义，基础设施层负责和数据库字段对接。
5. `MarketNode` 继续承担试算入口的编排职责，而不是把计算逻辑写死在一个方法里。

