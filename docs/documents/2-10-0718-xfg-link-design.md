# 2-10-0718-xfg-link-design 学习文档

## 一、分支目标

当前分支在上一阶段"拼团交易锁单"的基础上，新增**责任链抽象模版设计**框架，提供两套通用责任链模型（model1 / model2），用于支撑业务规则链的编排与执行。

核心目标是：将责任链模式从具体业务代码中抽离为可复用的框架，使各业务模块能够以统一的方式定义和串联规则处理器，降低重复编码成本。

## 二、新增功能概览

本分支新增文件全部属于 design framework 框架层或测试验证代码：

| 分类 | 位置 | 说明 |
|------|------|------|
| model1 框架 | `types/.../link/model1/` | 单实例链模型（处理器自维护 next 指针） |
| model2 框架 | `types/.../link/model2/` | 多实例链模型（链表统一管理处理器） |
| 测试代码 | `test/types/` | 两套模型的验证测试 |
| SQL 备份 | `docs/dev-ops/mysql/sql-bak/` | 数据库快照 |

### 新增文件清单（共 19 个文件）

**model1 框架（3 个文件）：**
- `types/.../design/framework/link/model1/ILogicLink.java`
- `types/.../design/framework/link/model1/ILogicChainArmory.java`
- `types/.../design/framework/link/model1/AbstractLogicLink.java`

**model2 框架（5 个文件）：**
- `types/.../design/framework/link/model2/LinkArmory.java`
- `types/.../design/framework/link/model2/chain/ILink.java`
- `types/.../design/framework/link/model2/chain/LinkedList.java`
- `types/.../design/framework/link/model2/chain/BusinessLinkedList.java`
- `types/.../design/framework/link/model2/handler/ILogicHandler.java`

**model1 测试（3 个文件）：**
- `app/src/test/.../types/rule01/factory/Rule01TradeRuleFactory.java`
- `app/src/test/.../types/rule01/logic/RuleLogic101.java`
- `app/src/test/.../types/rule01/logic/RuleLogic102.java`

**model2 测试（4 个文件）：**
- `app/src/test/.../types/rule02/factory/Rule02TradeRuleFactory.java`
- `app/src/test/.../types/rule02/logic/RuleLogic201.java`
- `app/src/test/.../types/rule02/logic/RuleLogic202.java`
- `app/src/test/.../types/rule02/logic/XxxResponse.java`

**集成测试（2 个文件）：**
- `app/src/test/.../types/Link01Test.java`
- `app/src/test/.../types/Link02Test.java`

**SQL 备份（1 个文件）：**
- `docs/dev-ops/mysql/sql-bak/2-10-group_buy_market.sql`

## 三、设计概览：两套责任链模型对比

| 维度 | model1（单实例链） | model2（多实例链） |
|------|---------------------|---------------------|
| 核心思想 | 每个处理器自己持有 next 指针 | 双向链表统一管理所有处理器 |
| 链即节点 | 链头 = 第一个处理器对象 | 链 = `BusinessLinkedList` 对象（本身实现 `ILogicHandler`）|
| 串联方式 | 手动 `appendNext()` 逐个链接 | `LinkArmory` 构造器一次性装配 |
| 执行方式 | 每个处理器调用 `next()` 驱动下一个 | 遍历链表 node，直到某个返回非 null |
| 适用场景 | 单例复用，链头即入口 | 每次请求构建新链实例，多链并存 |
| Spring 注册 | @Service 标注处理器，链头注入即可 | @Bean 方法注册整条链，多链可同时存在 |

## 四、model1：单实例链

### 设计思路

每个处理器（LogicHandler）自身就是一个链节点，内部持有下一个节点的引用。链头即第一个处理器，调用链头的 `apply()` 启动执行，通过 `next()` 方法驱动后续节点。

**核心接口/类：**

```
ILogicChainArmory          ILogicLink
┌─────────────────┐        ┌──────────────────────────┐
│ next()           │        │ apply(T, D): R          │
│ appendNext()     │        └──────────────────────────┘
└────────┬────────┘               ↑
         ↑                        │
         └────────────────────────┘
                  │
         AbstractLogicLink
         ┌──────────────────────────┐
         │ - next: ILogicLink       │
         │ + next(): ILogicLink     │
         │ + appendNext(): ILogicLink│
         │ # next(T, D): R          │
         └──────────────────────────┘
```

### ILogicChainArmory（链装配）

```java
public interface ILogicChainArmory<T, D, R> {
    ILogicLink<T, D, R> next();
    ILogicLink<T, D, R> appendNext(ILogicLink<T, D, R> next);
}
```

泛型参数：
- `T`：请求参数类型
- `D`：动态上下文类型
- `R`：处理结果类型

### ILogicLink（责任链接口）

```java
public interface ILogicLink<T, D, R> extends ILogicChainArmory<T, D, R> {
    R apply(T requestParameter, D dynamicContext) throws Exception;
}
```

继承 `ILogicChainArmory`，同时拥有链装配能力和业务执行能力。即每个节点既能处理逻辑，又能指向下一个节点。

### AbstractLogicLink（抽象实现）

```java
public abstract class AbstractLogicLink<T, D, R> implements ILogicLink<T, D, R> {

    private ILogicLink<T, D, R> next;

    public ILogicLink<T, D, R> next() {
        return next;
    }

    public ILogicLink<T, D, R> appendNext(ILogicLink<T, D, R> next) {
        this.next = next;
        return next;
    }

    protected R next(T requestParameter, D dynamicContext) throws Exception {
        return next.apply(requestParameter, dynamicContext);
    }
}
```

三个关键职责：
1. **维护 next 指针**：`appendNext()` 返回 next，支持链式编排。
2. **提供 next() 调用**：子类在 `apply()` 中调用 `next(requestParameter, dynamicContext)` 驱动下一个节点。
3. **子类只需重写 apply()**：具体的业务规则逻辑只需覆写 `apply` 方法。

### 使用示例（RuleLogic101 / RuleLogic102）

RuleLogic101（节点1，驱动到下一个节点）：

```java
@Service
public class RuleLogic101 extends AbstractLogicLink<String, DynamicContext, String> {
    public String apply(String requestParameter, DynamicContext dynamicContext) {
        log.info("link model01 RuleLogic101");
        return next(requestParameter, dynamicContext); // 传递给下一个节点
    }
}
```

RuleLogic102（节点2，终止节点）：

```java
@Service
public class RuleLogic102 extends AbstractLogicLink<String, DynamicContext, String> {
    public String apply(String requestParameter, DynamicContext dynamicContext) {
        log.info("link model01 RuleLogic102");
        return "link model01 单实例链"; // 返回最终结果，不再 next
    }
}
```

Rule01TradeRuleFactory（工厂串联）：

```java
@Service
public class Rule01TradeRuleFactory {
    @Resource private RuleLogic101 ruleLogic101;
    @Resource private RuleLogic102 ruleLogic102;

    public ILogicLink<String, DynamicContext, String> openLogicLink() {
        ruleLogic101.appendNext(ruleLogic102);
        return ruleLogic101; // 返回链头
    }
}
```

调用方式（Link01Test）：

```java
ILogicLink<String, DynamicContext, String> logicLink = factory.openLogicLink();
String result = logicLink.apply("123", new DynamicContext());
```

## 五、model2：多实例链

### 设计思路

不再让每个处理器维护 next 指针，而是用自实现的双向链表统一管理所有处理器。链本身（`BusinessLinkedList`）对外也是一个 `ILogicHandler`，可以像普通处理器一样被调用 `apply()`。遍历时逐个 node 执行，直到某个处理器返回非 null 结果则终止。

**层级关系：**

```
ILogicHandler                      ILink<E>
┌──────────────┐                   ┌──────────────┐
│ apply(T,D):R │                   │ add(E)       │
│ next(T,D):R  │                   │ remove(O)    │
└──────┬───────┘                   │ get(int):E   │
       ↑                           └──────┬───────┘
       │                                  ↑
       │                           ┌──────┴───────┐
       │                           │ LinkedList<E>│
       │                           │ - first: Node│
       │                           │ - last: Node │
       │                           │ - size: int  │
       │                           └──────┬───────┘
       │                                  ↑
BusinessLinkedList                          │
┌──────────────────────────┐     ┌─────────┴────────────┐
│ extends LinkedList       │     │ extends LinkedList    │
│ implements ILogicHandler │──>──│                       │
│                          │     │ LinkArmory            │
│ apply(): 遍历first→last  │     │ (构造器装配链)         │
└──────────────────────────┘     └──────────────────────┘
```

### ILogicHandler（逻辑处理器）

```java
public interface ILogicHandler<T, D, R> {
    default R next(T requestParameter, D dynamicContext) {
        return null;
    }
    R apply(T requestParameter, D dynamicContext) throws Exception;
}
```

关键：`next()` 是默认方法返回 null，子类可以不实现。`apply()` 是子类必须实现的业务逻辑。

### LinkedList<E>（自实现双向链表）

这是从零实现的双向链表，并非 JDK 的 `java.util.LinkedList`。核心实现：

**数据结构：**

```java
transient Node<E> first;
transient Node<E> last;
transient int size = 0;

static class Node<E> {
    E item;
    Node<E> next;
    Node<E> prev;
}
```

**核心方法：**

| 方法 | 实现要点 |
|------|----------|
| `linkFirst(E)` | 头插，维护 first/last 指针和 prev/next 双向关联 |
| `linkLast(E)` | 尾插，维护 first/last 指针和 prev/next 双向关联 |
| `unlink(Node<E>)` | 删除节点，重建相邻节点的双向引用，清理被删节点的引用 |
| `node(int index)` | 折半查找——index < size/2 从头遍历，否则从尾遍历 |
| `remove(Object)` | 遍历找节点后调用 unlink |

### BusinessLinkedList（业务执行链）

```java
public class BusinessLinkedList<T, D, R>
    extends LinkedList<ILogicHandler<T, D, R>>
    implements ILogicHandler<T, D, R> {

    public BusinessLinkedList(String name) {
        super(name);
    }

    public R apply(T requestParameter, D dynamicContext) throws Exception {
        Node<ILogicHandler<T, D, R>> current = this.first;
        do {
            ILogicHandler<T, D, R> item = current.item;
            R apply = item.apply(requestParameter, dynamicContext);
            if (null != apply) return apply; // 非null即终止
            current = current.next;
        } while (null != current);
        return null;
    }
}
```

执行逻辑：
1. 从链表头节点开始遍历。
2. 依次调用每个 `ILogicHandler` 的 `apply()` 方法。
3. **如果返回非 null**：立即返回该结果，不再执行后续节点（短路机制）。
4. **如果返回 null**：继续执行下一个节点（由处理器的 `next()` 默认返回 null 驱动）。

### LinkArmory（链装配工厂）

```java
public class LinkArmory<T, D, R> {
    private final BusinessLinkedList<T, D, R> logicLink;

    @SafeVarargs
    public LinkArmory(String linkName, ILogicHandler<T, D, R>... logicHandlers) {
        logicLink = new BusinessLinkedList<>(linkName);
        for (ILogicHandler<T, D, R> logicHandler : logicHandlers) {
            logicLink.add(logicHandler);
        }
    }

    public BusinessLinkedList<T, D, R> getLogicLink() {
        return logicLink;
    }
}
```

通过可变参数接收处理器数组，逐个加入链表，一次调用完成链的构建。

### 使用示例（RuleLogic201 / RuleLogic202）

RuleLogic201（节点1，调用 next 驱动下一个节点）：

```java
@Service
public class RuleLogic201 implements ILogicHandler<String, DynamicContext, XxxResponse> {
    public XxxResponse apply(String requestParameter, DynamicContext dynamicContext) {
        log.info("link model02 RuleLogic201");
        return next(requestParameter, dynamicContext); // 返回null，驱动下一个
    }
}
```

RuleLogic202（节点2，终止节点）：

```java
@Service
public class RuleLogic202 implements ILogicHandler<String, DynamicContext, XxxResponse> {
    public XxxResponse apply(String requestParameter, DynamicContext dynamicContext) {
        log.info("link model02 RuleLogic202");
        return new XxxResponse("hi 小傅哥！"); // 返回非null，终止链
    }
}
```

链构建（Rule02TradeRuleFactory）：

```java
@Service
public class Rule02TradeRuleFactory {
    @Bean("demo01")
    public BusinessLinkedList<String, DynamicContext, XxxResponse> demo01(
            RuleLogic201 ruleLogic201, RuleLogic202 ruleLogic202) {
        LinkArmory<String, DynamicContext, XxxResponse> linkArmory =
            new LinkArmory<>("demo01", ruleLogic201, ruleLogic202);
        return linkArmory.getLogicLink();
    }

    @Bean("demo02")
    public BusinessLinkedList<String, DynamicContext, XxxResponse> demo02(
            RuleLogic202 ruleLogic202) {
        LinkArmory<String, DynamicContext, XxxResponse> linkArmory =
            new LinkArmory<>("demo02", ruleLogic202);
        return linkArmory.getLogicLink();
    }
}
```

调用方式（Link02Test）：

```java
@Resource(name = "demo01")
private BusinessLinkedList<String, DynamicContext, XxxResponse> businessLinkedList01;

XxxResponse result = businessLinkedList01.apply("123", new DynamicContext());
```

## 六、两种模型的责任链执行流程对比

### model1 流程

```
RuleLogic101.apply()
  -> 执行自身逻辑
  -> next(requestParam, ctx)
    -> RuleLogic102.apply()
      -> 执行自身逻辑
      -> return "结果字符串"   (不再调用 next，链终止)
```

每个处理器决定是否继续调用 `next()`。不调用 `next()` 则链终止。

### model2 流程

```
BusinessLinkedList.apply()
  -> for each node in linked list:
    RuleLogic201.apply() -> next() -> return null  (null，继续)
    RuleLogic202.apply() -> return XxxResponse     (非null，终止链)
  -> return XxxResponse
```

链遍历所有节点，但任一节点返回非 null 立即终止（短路机制）。处理器的 `next()` 方法默认返回 null，表示"我处理完了，继续下一个"。

## 七、model1 与 model2 的核心区别

| 维度 | model1 | model2 |
|------|--------|--------|
| 链结构持有者 | 每个节点自己的 `next` 字段 | `BusinessLinkedList` 统一持有 `first/last` |
| 链与节点关系 | 链就是节点（链头即入口） | 链是独立对象（包装了节点列表） |
| 驱动下一节点 | 子类显式调用 `next()` | 链表遍历时自动驱动 |
| 终止条件 | 子类不调用 `next()` | `apply()` 返回非 null |
| 多链并存 | 单实例模式，链头唯一 | `@Bean` 可注册多条链（demo01/demo02） |
| 链的装配 | 编码时 `appendNext()` 串联 | `LinkArmory` 构造时一次性装配 |
| 链表操作 | 无（只有一个 next 指针） | 支持 add/remove/get/printLinkList |

## 八、框架泛型设计说明

两套模型使用相同的三泛型参数：

```
<T, D, R>
```

| 泛型 | 含义 | 示例 |
|------|------|------|
| `T` | 请求参数类型 | `String`、`MarketProductEntity` |
| `D` | 动态上下文类型 | 可在责任链节点间传递共享数据 |
| `R` | 处理结果类型 | `String`、`XxxResponse`、`MarketPayOrderEntity` |

这个设计使框架可以适配任意业务场景——只要确定好入参类型、上下文类型和返回结果类型即可。

## 九、与现有试算责任链的关联

当前项目中试算模块已有责任链实现（`SwitchNode` -> `TagNode` -> `MarketNode` -> `EndNode`），但它们是硬编码在具体业务节点中的。本节新增的 model1/model2 框架提供了**通用的责任链模板**，后续可以：

1. 将试算责任链迁移到 model2 框架，用 `LinkArmory` 构建节点链。
2. 将交易锁单中的规则校验（幂等检查、进度预检）迁移到 model1 框架。
3. 其他业务模块（活动匹配、优惠计算）也可以用统一的框架快速搭建规则链。

## 十、测试覆盖

### Link01Test（验证 model1）

```java
ILogicLink<String, DynamicContext, String> logicLink = rule01TradeRuleFactory.openLogicLink();
String logic = logicLink.apply("123", new DynamicContext());
```

预期：依次经过 RuleLogic101 → RuleLogic102，最终返回 `"link model01 单实例链"`。

### Link02Test（验证 model2）

```java
// demo01: RuleLogic201 + RuleLogic202
XxxResponse result = businessLinkedList01.apply("123", new DynamicContext());

// demo02: 仅 RuleLogic202
XxxResponse result = businessLinkedList02.apply("123", new DynamicContext());
```

demo01 预期依次经过 RuleLogic201（返回 null，继续）→ RuleLogic202（返回 XxxResponse，终止）。demo02 仅一个节点，直接返回结果。

## 十一、代码走读顺序建议

建议按以下顺序学习：

1. `model1/ILogicChainArmory.java`：理解链装配的最小接口。
2. `model1/ILogicLink.java`：理解 combine 装配 + 执行的完整接口。
3. `model1/AbstractLogicLink.java`：理解 next 指针维护和 `next()` 驱动方法。
4. `rule01/logic/RuleLogic101.java` / `RuleLogic102.java`：理解 model1 的具体用法。
5. `rule01/factory/Rule01TradeRuleFactory.java`：理解如何串联链节点。
6. `Link01Test.java`：看 model1 的完整调用链路。

7. `model2/chain/ILink.java`：理解链表基础接口。
8. `model2/chain/LinkedList.java`：重点阅读自实现双向链表（头插、尾插、折半查找）。
9. `model2/handler/ILogicHandler.java`：理解处理器接口（`apply` + 默认 `next`）。
10. `model2/chain/BusinessLinkedList.java`：理解业务链如何遍历节点 + 短路终止。
11. `model2/LinkArmory.java`：理解链装配工厂。
12. `rule02/logic/RuleLogic201.java` / `RuleLogic202.java`：理解 model2 的具体用法。
13. `rule02/factory/Rule02TradeRuleFactory.java`：理解多链 Bean 注册方式。
14. `Link02Test.java`：看 model2 的完整调用链路。

## 十二、设计要点总结

1. **model1 的目标是"轻量级单链"**：链就是节点，链头即入口，适合固定不变的单一规则链场景。
2. **model2 的目标是"灵活多链"**：链和节点分离，同一条链可多次构建不同实例，`@Bean` 可注册多条链并存。
3. **自实现 LinkedList 的原因**：方便在遍历过程中控制执行逻辑（短路返回），且不依赖 JDK 的实现细节。
4. **Model2 的短路机制**：`BusinessLinkedList.apply()` 中 `if (null != apply) return apply`，相当于"第一个人处理完就不用后面的人了"。
5. **泛型设计通用化**：三泛型 `<T, D, R>` 可适配任意业务场景的参数、上下文和返回类型。
6. **两套模型互补**：model1 适合"现有节点串一条链"的快拼场景，model2 适合"工厂批量生产链"的规模化场景。
