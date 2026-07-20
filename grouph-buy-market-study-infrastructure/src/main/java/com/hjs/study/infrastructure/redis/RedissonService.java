package com.hjs.study.infrastructure.redis;

import org.redisson.api.*;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * `IRedisService` 的 Redisson 实现。
 * <p>
 * 该类把项目中定义的 Redis 抽象接口，具体落地为 RedissonClient 的调用。
 * 也就是说，上层看到的是统一的 `IRedisService`，底层这里负责把每一种数据结构、
 * 锁模型和并发原语映射到 Redisson 的具体对象。
 * <p>
 * 由于 Redisson 已经对序列化、分布式锁、原子计数器、BitSet 等能力做了很好封装，
 * 因此本实现大多数方法都采用“轻包装转发”的方式，尽量保持语义清晰。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 */
@Service("redissonService")
public class RedissonService implements IRedisService {

    /** Redisson 客户端，是所有 Redis 数据结构与分布式并发能力的入口。 */
    @Resource
    private RedissonClient redissonClient;

    /** 写入一个永久 KV 值。 */
    public <T> void setValue(String key, T value) {
        redissonClient.<T>getBucket(key).set(value);
    }

    @Override
    /** 写入一个带过期时间的 KV 值。 */
    public <T> void setValue(String key, T value, long expired) {
        RBucket<T> bucket = redissonClient.getBucket(key);
        bucket.set(value, Duration.ofMillis(expired));
    }

    /** 读取一个 KV 值。 */
    public <T> T getValue(String key) {
        return redissonClient.<T>getBucket(key).get();
    }

    @Override
    /** 获取普通队列对象。 */
    public <T> RQueue<T> getQueue(String key) {
        return redissonClient.getQueue(key);
    }

    @Override
    /** 获取阻塞队列对象。 */
    public <T> RBlockingQueue<T> getBlockingQueue(String key) {
        return redissonClient.getBlockingQueue(key);
    }

    @Override
    /** 基于阻塞队列创建延迟队列。 */
    public <T> RDelayedQueue<T> getDelayedQueue(RBlockingQueue<T> rBlockingQueue) {
        return redissonClient.getDelayedQueue(rBlockingQueue);
    }

    @Override
    /** 设置原子长整型初始值。 */
    public void setAtomicLong(String key, long value) {
        redissonClient.getAtomicLong(key).set(value);
    }

    @Override
    /** 读取原子长整型当前值。 */
    public Long getAtomicLong(String key) {
        return redissonClient.getAtomicLong(key).get();
    }

    @Override
    /** 对原子长整型自增 1。 */
    public long incr(String key) {
        return redissonClient.getAtomicLong(key).incrementAndGet();
    }

    @Override
    /** 对原子长整型按指定步长自增。 */
    public long incrBy(String key, long delta) {
        return redissonClient.getAtomicLong(key).addAndGet(delta);
    }

    @Override
    /** 对原子长整型自减 1。 */
    public long decr(String key) {
        return redissonClient.getAtomicLong(key).decrementAndGet();
    }

    @Override
    /** 对原子长整型按指定步长自减。 */
    public long decrBy(String key, long delta) {
        return redissonClient.getAtomicLong(key).addAndGet(-delta);
    }

    @Override
    /** 删除指定 key 对应的数据。 */
    public void remove(String key) {
        redissonClient.getBucket(key).delete();
    }

    @Override
    /** 判断指定 key 是否存在。 */
    public boolean isExists(String key) {
        return redissonClient.getBucket(key).isExists();
    }

    /** 向 Set 写入一个元素。 */
    public void addToSet(String key, String value) {
        RSet<String> set = redissonClient.getSet(key);
        set.add(value);
    }

    /** 判断某个元素是否在 Set 中。 */
    public boolean isSetMember(String key, String value) {
        RSet<String> set = redissonClient.getSet(key);
        return set.contains(value);
    }

    /** 向 List 追加元素。 */
    public void addToList(String key, String value) {
        RList<String> list = redissonClient.getList(key);
        list.add(value);
    }

    /** 读取 List 指定下标的元素。 */
    public String getFromList(String key, int index) {
        RList<String> list = redissonClient.getList(key);
        return list.get(index);
    }

    @Override
    /** 获取 Map 结构对象。 */
    public <K, V> RMap<K, V> getMap(String key) {
        return redissonClient.getMap(key);
    }

    /** 向 Map 中写入一个字段值。 */
    public void addToMap(String key, String field, String value) {
        RMap<String, String> map = redissonClient.getMap(key);
        map.put(field, value);
    }

    /** 读取 Map 中指定字段的字符串值。 */
    public String getFromMap(String key, String field) {
        RMap<String, String> map = redissonClient.getMap(key);
        return map.get(field);
    }

    @Override
    /** 读取 Map 中指定字段的泛型值。 */
    public <K, V> V getFromMap(String key, K field) {
        return redissonClient.<K, V>getMap(key).get(field);
    }

    /** 向有序集合写入元素。 */
    public void addToSortedSet(String key, String value) {
        RSortedSet<String> sortedSet = redissonClient.getSortedSet(key);
        sortedSet.add(value);
    }

    @Override
    /** 获取可重入锁。 */
    public RLock getLock(String key) {
        return redissonClient.getLock(key);
    }

    @Override
    /** 获取公平锁。 */
    public RLock getFairLock(String key) {
        return redissonClient.getFairLock(key);
    }

    @Override
    /** 获取读写锁。 */
    public RReadWriteLock getReadWriteLock(String key) {
        return redissonClient.getReadWriteLock(key);
    }

    @Override
    /** 获取信号量。 */
    public RSemaphore getSemaphore(String key) {
        return redissonClient.getSemaphore(key);
    }

    @Override
    /** 获取可过期信号量。 */
    public RPermitExpirableSemaphore getPermitExpirableSemaphore(String key) {
        return redissonClient.getPermitExpirableSemaphore(key);
    }

    @Override
    /** 获取分布式闭锁。 */
    public RCountDownLatch getCountDownLatch(String key) {
        return redissonClient.getCountDownLatch(key);
    }

    @Override
    /** 获取布隆过滤器。 */
    public <T> RBloomFilter<T> getBloomFilter(String key) {
        return redissonClient.getBloomFilter(key);
    }

    @Override
    /**
     * 以 `trySet` 的方式实现一个轻量级 NX 锁。
     * <p>
     * 成功表示当前调用方抢占到了该 key，失败表示已经有其他调用方持有。
     */
    public Boolean setNx(String key) {
        return redissonClient.getBucket(key).trySet("lock");
    }

    @Override
    /** 以带过期时间的 `trySet` 方式实现轻量级 NX 锁。 */
    public Boolean setNx(String key, long expired, TimeUnit timeUnit) {
        return redissonClient.getBucket(key).trySet("lock", expired, timeUnit);
    }

    @Override
    /** 获取 BitSet，常用于用户标签位图判断。 */
    public RBitSet getBitSet(String key) {
        return redissonClient.getBitSet(key);
    }

}
