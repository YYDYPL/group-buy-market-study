package com.hjs.study.infrastructure.adapter.repository;

import com.hjs.study.infrastructure.dcc.DCCService;
import com.hjs.study.infrastructure.redis.IRedisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;
import java.util.function.Supplier;
/**
 * 仓储实现公共基类。
 * <p>
 * 当前项目中的多个仓储都需要使用 Redis 缓存和 DCC 开关能力，
 * 为避免每个仓储都重复编写“先查缓存、未命中再查数据库、再回填缓存”的模板代码，
 * 抽出该基类统一承载这部分通用逻辑。
 *
 * @author Fuzhengwei bugstack.cn @小傅哥
 * @description 仓储抽象类
 */
public abstract class AbstractRepository {

    /** 基类日志组件，用于记录缓存降级等通用行为。 */
    private final Logger logger = LoggerFactory.getLogger(AbstractRepository.class);

    /** Redis 基础能力封装，负责缓存读写。 */
    @Resource
    protected IRedisService redisService;

    /** DCC 动态配置中心，用于控制缓存开关、降级策略等运行时行为。 */
    @Resource
    protected DCCService dccService;

    /**
     * 通用缓存查询模板方法。
     * <p>
     * 执行顺序为：先判断缓存开关是否开启；
     * 开启则优先查缓存，缓存未命中时回源数据库并写回缓存；
     * 未开启则直接走数据库，并记录一条缓存降级日志。
     *
     * @param cacheKey 缓存键
     * @param dbFallback 数据库回源查询函数
     * @param <T> 返回值类型
     * @return 查询结果
     */
    protected <T> T getFromCacheOrDb(String cacheKey, Supplier<T> dbFallback) {
        // 判断是否开启缓存
        if (dccService.isCacheOpenSwitch()) {
            // 从缓存获取
            T cacheResult = redisService.getValue(cacheKey);
            // 缓存存在则直接返回
            if (null != cacheResult) {
                return cacheResult;
            }
            // 缓存不存在则从数据库获取
            T dbResult = dbFallback.get();
            // 数据库查询结果为空则直接返回
            if (null == dbResult) {
                return null;
            }
            // 写入缓存
            redisService.setValue(cacheKey, dbResult);
            return dbResult;
        } else {
            // 缓存未开启，直接从数据库获取
            logger.warn("缓存降级 {}", cacheKey);
            return dbFallback.get();
        }
    }

    /**
     * 通用缓存查询模板方法，支持显式指定缓存过期时间。
     * <p>
     * 适用于需要控制缓存生命周期的场景，例如某些统计类或时效性较强的数据。
     *
     * @param cacheKey 缓存键
     * @param dbFallback 数据库回源查询函数
     * @param expired 缓存过期时间
     * @param <T> 返回值类型
     * @return 查询结果
     */
    protected <T> T getFromCacheOrDb(String cacheKey, Supplier<T> dbFallback, long expired) {
        // 判断是否开启缓存
        if (dccService.isCacheOpenSwitch()) {
            // 从缓存获取
            T cacheResult = redisService.getValue(cacheKey);
            // 缓存存在则直接返回
            if (null != cacheResult) {
                return cacheResult;
            }
            // 缓存不存在则从数据库获取
            T dbResult = dbFallback.get();
            // 数据库查询结果为空则直接返回
            if (null == dbResult) {
                return null;
            }
            // 写入缓存（带过期时间）
            redisService.setValue(cacheKey, dbResult, expired);
            return dbResult;
        } else {
            // 缓存未开启，直接从数据库获取
            logger.warn("缓存降级 {}", cacheKey);
            return dbFallback.get();
        }
    }

}
