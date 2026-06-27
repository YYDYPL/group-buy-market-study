# 2-5-0627-xfg-crowd-tags 学习文档

## 一、分支目标

当前分支在上一阶段“优惠金额计算策略模式”的基础上，新增了人群标签能力。它的核心目标是把用户是否属于某个人群标签沉淀为可查询的数据能力，为后续 `TAG` 类型优惠、人群可见、人群参与限制等功能做准备。

本分支主要新增了四块内容：

1. 人群标签领域服务：执行标签批次任务，把用户写入人群标签。
2. 标签仓储与 MyBatis 持久化：维护标签、标签任务、标签明细三类数据。
3. Redis/Redisson 能力封装：用 Redis BitSet 存储和查询用户是否命中标签。
4. 测试用例和数据库脚本：提供标签任务执行与 bitmap 命中验证入口。

## 二、新增功能概览

### 1. 人群标签领域服务

新增文件：

- `grouph-buy-market-study-domain/src/main/java/com/hjs/study/domain/tag/ITagService.java`
- `grouph-buy-market-study-domain/src/main/java/com/hjs/study/domain/tag/TagService.java`
- `grouph-buy-market-study-domain/src/main/java/com/hjs/study/domain/tag/model/entity/CrowdTagsJobEntity.java`

`ITagService` 定义标签任务执行入口：

```java
void execTagBatchJob(String tagId, String batchId);
```

`TagService` 是当前分支的业务编排类，执行流程如下：

```text
execTagBatchJob(tagId, batchId)
  -> 查询标签批次任务
  -> 采集用户数据
  -> 写入人群标签明细
  -> 更新人群标签统计量
```

当前版本里，用户采集逻辑还是模拟数据：

```java
List<String> userIdList = new ArrayList<String>() {{
    add("xiaofuge");
    add("liergou");
}};
```

这说明当前分支重点是搭建人群标签写入与查询链路，真实用户行为数据采集会在后续功能里补充。

### 2. 标签任务实体

`CrowdTagsJobEntity` 是领域层标签任务对象，包含：

- `tagType`：标签类型，比如参与量、消费金额。
- `tagRule`：标签规则，比如限定 N 次。
- `statStartTime`：统计开始时间。
- `statEndTime`：统计结束时间。

领域层只保留执行任务需要关心的字段，不暴露数据库 PO 的全部字段。

## 三、仓储层设计

### 1. 领域仓储接口

新增文件：

- `grouph-buy-market-study-domain/src/main/java/com/hjs/study/domain/tag/adapter/repository/ITagRepository.java`

接口定义了标签领域需要的三类能力：

```java
CrowdTagsJobEntity queryCrowdTagsJobEntity(String tagId, String batchId);

void addCrowdTagsUserId(String tagId, String userId);

void updateCrowdTagsStatistics(String tagId, int count);
```

领域服务只依赖该接口，不直接感知 MyBatis、Redis 或表结构。

### 2. 基础设施仓储实现

新增文件：

- `grouph-buy-market-study-infrastructure/src/main/java/com/hjs/study/infrastructure/adapter/repository/TagRepository.java`

`TagRepository` 负责把领域接口适配到 MySQL 和 Redis。

#### 查询标签任务

`queryCrowdTagsJobEntity` 会根据 `tagId` 和 `batchId` 查询 `crowd_tags_job` 表：

```java
CrowdTagsJob crowdTagsJobRes = crowdTagsJobDao.queryCrowdTagsJob(crowdTagsJobReq);
```

查到结果后转换为领域实体 `CrowdTagsJobEntity`。

#### 写入标签用户

`addCrowdTagsUserId` 同时做两件事：

1. 插入 MySQL 标签明细表 `crowd_tags_detail`。
2. 写入 Redis BitSet。

核心逻辑：

```java
crowdTagsDetailDao.addCrowdTagsUserId(crowdTagsDetailReq);

RBitSet bitSet = redisService.getBitSet(tagId);
bitSet.set(redisService.getIndexFromUserId(userId), true);
```

Redis key 直接使用 `tagId`，bit 位下标由 `userId` 通过 MD5 哈希计算得到。

#### 更新标签统计量

`updateCrowdTagsStatistics` 会把本次新增用户数累加到 `crowd_tags.statistics`：

```sql
update crowd_tags
set statistics = statistics + #{statistics}
where tag_id = #{tagId}
```

### 3. 重复写入处理

`crowd_tags_detail` 表对 `(tag_id, user_id)` 建了唯一索引。`TagRepository` 捕获 `DuplicateKeyException` 并忽略，避免重复执行任务时因重复用户导致流程失败。

当前实现的一个边界是：Redis BitSet 写入发生在 MySQL 插入成功之后。如果数据库已经有明细但 Redis 为空，重复执行时会命中唯一索引并跳过 Redis 回填。

## 四、Redis/Redisson 支撑

### 1. Redis 配置类

新增文件：

- `grouph-buy-market-study-app/src/main/java/com/hjs/study/config/RedisClientConfig.java`
- `grouph-buy-market-study-app/src/main/java/com/hjs/study/config/RedisClientConfigProperties.java`

`RedisClientConfigProperties` 读取配置前缀：

```java
@ConfigurationProperties(prefix = "redis.sdk.config")
```

`RedisClientConfig` 根据这些配置创建 `RedissonClient`：

```java
config.useSingleServer()
      .setAddress("redis://" + properties.getHost() + ":" + properties.getPort())
      .setConnectionPoolSize(properties.getPoolSize())
      .setConnectionMinimumIdleSize(properties.getMinIdleSize());
```

当前配置使用单机 Redis，并使用 `JsonJacksonCodec` 作为 Redisson codec。

### 2. application-dev.yml 配置

修改文件：

- `grouph-buy-market-study-app/src/main/resources/application-dev.yml`

新增 Redis 配置：

```yaml
redis:
  sdk:
    config:
      host: 124.223.115.249
      port: 16379
      pool-size: 10
      min-idle-size: 5
      idle-timeout: 30000
      connect-timeout: 5000
      retry-attempts: 3
      retry-interval: 1000
      ping-interval: 60000
      keep-alive: true
```

### 3. Redis 服务封装

新增文件：

- `grouph-buy-market-study-infrastructure/src/main/java/com/hjs/study/infrastructure/redis/IRedisService.java`
- `grouph-buy-market-study-infrastructure/src/main/java/com/hjs/study/infrastructure/redis/RedissonService.java`

`IRedisService` 封装了常见 Redis 能力：

- String bucket：`setValue`、`getValue`
- Queue / BlockingQueue / DelayedQueue
- AtomicLong：`incr`、`decr`
- Set / List / Map / SortedSet
- Lock / FairLock / ReadWriteLock
- Semaphore / CountDownLatch
- BloomFilter
- BitSet

本分支人群标签核心使用的是：

```java
RBitSet getBitSet(String key);
```

以及默认方法：

```java
default int getIndexFromUserId(String userId)
```

该方法用 MD5 对 `userId` 做哈希，再对 `Integer.MAX_VALUE` 取模，得到稳定的 bitmap 下标。

`RedissonService` 是 `IRedisService` 的 Redisson 实现，所有方法最终都委托给 `RedissonClient`。

## 五、MyBatis 与数据库表结构

### 1. DAO 接口

新增文件：

- `grouph-buy-market-study-infrastructure/src/main/java/com/hjs/study/infrastructure/dao/ICrowdTagsDao.java`
- `grouph-buy-market-study-infrastructure/src/main/java/com/hjs/study/infrastructure/dao/ICrowdTagsDetailDao.java`
- `grouph-buy-market-study-infrastructure/src/main/java/com/hjs/study/infrastructure/dao/ICrowdTagsJobDao.java`

三个 DAO 分别对应三类动作：

- `ICrowdTagsJobDao.queryCrowdTagsJob`：查询标签任务。
- `ICrowdTagsDetailDao.addCrowdTagsUserId`：插入标签用户明细。
- `ICrowdTagsDao.updateCrowdTagsStatistics`：更新标签统计量。

### 2. Mapper 文件

新增文件：

- `grouph-buy-market-study-app/src/main/resources/mybatis/mapper/crowd_tags_mapper.xml`
- `grouph-buy-market-study-app/src/main/resources/mybatis/mapper/crowd_tags_detail_mapper.xml`
- `grouph-buy-market-study-app/src/main/resources/mybatis/mapper/crowd_tags_job_mapper.xml`

Mapper 重点 SQL：

```sql
select tag_type, tag_rule, stat_start_time, stat_end_time
from crowd_tags_job
where tag_id = #{tagId} and batch_id = #{batchId}
```

```sql
insert into crowd_tags_detail(tag_id, user_id, create_time, update_time)
values (#{tagId}, #{userId}, now(), now())
```

```sql
update crowd_tags
set statistics = statistics + #{statistics}
where tag_id = #{tagId}
```

### 3. PO 对象

新增文件：

- `grouph-buy-market-study-infrastructure/src/main/java/com/hjs/study/infrastructure/dao/po/CrowdTags.java`
- `grouph-buy-market-study-infrastructure/src/main/java/com/hjs/study/infrastructure/dao/po/CrowdTagsDetail.java`
- `grouph-buy-market-study-infrastructure/src/main/java/com/hjs/study/infrastructure/dao/po/CrowdTagsJob.java`

PO 与数据库表一一对应，用于 MyBatis 映射。

### 4. SQL 脚本

新增和修改文件：

- `docs/dev-ops/mysql/sql-bak/2-5-group_buy_market.sql`
- `docs/dev-ops/mysql/sql/group_buy_market.sql`

新增三张表：

| 表名 | 作用 |
| --- | --- |
| `crowd_tags` | 人群标签主表，保存标签名称、描述和统计量。 |
| `crowd_tags_detail` | 人群标签明细表，保存某个用户属于某个标签。 |
| `crowd_tags_job` | 人群标签任务表，保存批次任务、标签规则和统计时间范围。 |

关键索引：

- `crowd_tags.uq_tag_id`：保证标签 ID 唯一。
- `crowd_tags_detail.uq_tag_user`：保证同一标签下同一用户只写入一次。
- `crowd_tags_job.uq_batch_id`：保证批次任务唯一。

初始化数据里提供了测试标签：

- `tagId = RQ_KJHKL98UU78H66554GFDV`
- `batchId = 10001`
- 测试用户：`xiaofuge`、`liergou`

## 六、依赖变更

修改文件：

- `pom.xml`
- `grouph-buy-market-study-app/pom.xml`
- `grouph-buy-market-study-infrastructure/pom.xml`

根 POM 在 `dependencyManagement` 中新增 Redisson 版本管理：

```xml
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson-spring-boot-starter</artifactId>
    <version>3.26.0</version>
</dependency>
```

`app` 和 `infrastructure` 模块引入 `redisson-spring-boot-starter`，分别用于启动配置和基础设施服务实现。

## 七、测试入口

新增文件：

- `grouph-buy-market-study-app/src/test/java/com/hjs/study/test/domain/tag/ITagServiceTest.java`

测试类包含两个用例。

### 1. 执行标签任务

```java
tagService.execTagBatchJob("RQ_KJHKL98UU78H66554GFDV", "10001");
```

该用例会执行完整链路：

```text
TagService
  -> TagRepository
  -> crowd_tags_job 查询任务
  -> crowd_tags_detail 写入用户
  -> Redis BitSet 写入用户位图
  -> crowd_tags 更新统计量
```

### 2. 查询 Redis BitSet

```java
RBitSet bitSet = redisService.getBitSet("RQ_KJHKL98UU78H66554GFDV");
bitSet.get(redisService.getIndexFromUserId("xiaofuge"));
bitSet.get(redisService.getIndexFromUserId("gudebai"));
```

预期结果：

- `xiaofuge`：存在，返回 `true`。
- `gudebai`：不存在，返回 `false`。

## 八、与上一阶段的关系

上一阶段 `2-4-0620-xfg-discount-calculate` 已经在折扣计算模板中预留了 `TAG` 类型判断：

```java
if (DiscountTypeEnum.TAG.equals(groupBuyDiscount.getDiscountType())) {
    boolean isCrowdRange = filterTagId(userId, groupBuyDiscount.getTagId());
    if (!isCrowdRange) return originalPrice;
}
```

当前分支新增的人群标签能力，正是为后续实现 `filterTagId` 提供数据基础。

当前版本还没有把 `AbstractDiscountCalculateService.filterTagId` 真正接入 Redis BitSet 查询，因此本分支可以理解为“先建设人群标签数据生产和查询能力”，下一步再把它接到折扣计算、人群可见或参与限制中。

## 九、代码走读顺序建议

建议按这个顺序学习：

1. `ITagService` 和 `TagService`：先理解领域入口和业务流程。
2. `ITagRepository`：看领域层需要哪些数据能力。
3. `TagRepository`：看 MySQL 和 Redis 如何被组合起来。
4. `IRedisService` 和 `RedissonService`：理解 Redis 通用能力封装，重点看 BitSet。
5. `RedisClientConfig` 和 `RedisClientConfigProperties`：看 RedissonClient 如何初始化。
6. 三个 DAO 与 Mapper：把仓储方法和 SQL 对上。
7. `group_buy_market.sql`：看标签表结构和测试数据。
8. `ITagServiceTest`：按测试用例跑完整链路。

## 十、设计要点总结

1. 领域服务只编排业务动作，不直接操作 MySQL 或 Redis。
2. 仓储实现负责把领域动作落到 MySQL 明细和 Redis bitmap。
3. MySQL 负责持久化和唯一约束，Redis BitSet 负责高效命中判断。
4. 使用 `tagId` 作为 BitSet key，使用 `userId` 哈希值作为 bit 下标。
5. Redisson 被封装为 `IRedisService`，后续业务无需直接依赖 `RedissonClient`。
6. 当前分支为 `TAG` 类型优惠打基础，但还没有完成与折扣计算模板的最终联动。

