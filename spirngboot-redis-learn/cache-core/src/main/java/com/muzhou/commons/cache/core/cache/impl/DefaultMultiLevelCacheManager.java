package com.muzhou.commons.cache.core.cache.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.muzhou.commons.cache.autoconfigure.properties.CacheProperties;
import com.muzhou.commons.cache.core.cache.MultiLevelCacheManager;
import com.muzhou.commons.cache.core.hotkey.HotKeyManager;
import com.muzhou.commons.cache.core.lock.DistributedLock;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.redis.core.RedisTemplate;

import javax.cache.CacheException;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class DefaultMultiLevelCacheManager implements MultiLevelCacheManager, InitializingBean {

    private final RedisTemplate<String, Object> redisTemplate;
    private final HotKeyManager hotKeyManager;
    private final CacheProperties cacheProperties;
    private final DistributedLock distributedLock;
    private Cache<Object, Object> localCache;

    public DefaultMultiLevelCacheManager(
            RedisTemplate<String, Object> redisTemplate,
            HotKeyManager hotKeyManager,
            CacheProperties cacheProperties,
            DistributedLock distributedLock) {
        this.redisTemplate = redisTemplate;
        this.hotKeyManager = hotKeyManager;
        this.cacheProperties = cacheProperties;
        this.distributedLock = distributedLock;
    }

    @Override
    public void afterPropertiesSet() {
        this.localCache = buildLocalCache();
    }

    private Cache<Object, Object> buildLocalCache() {
        Caffeine<Object, Object> builder = Caffeine.newBuilder()
                .expireAfterWrite(cacheProperties.getLocal().getExpireAfterWrite())
                .maximumSize(cacheProperties.getLocal().getMaximumSize())
                .recordStats();

        if (cacheProperties.getLocal().getRefreshAfterWrite() != null) {
            builder.refreshAfterWrite(cacheProperties.getLocal().getRefreshAfterWrite());
        }

        return builder.build();
    }

    @Override
    public <T> T get(String key, Callable<T> valueLoader, long ttlSeconds) {
        try {
            // 记录热点key访问
            hotKeyManager.recordAccess(key);

            // 1. 先查本地缓存
            T value = (T) localCache.get(key, k -> {
                // 2. 本地没有则查Redis
                T redisValue = (T) redisTemplate.opsForValue().get(key);
                if (redisValue != null) {
                    return redisValue;
                }

                // 3. Redis也没有，加分布式锁查数据库
                String lockKey = "lock:" + key;
                try {
                    if (distributedLock.tryLock(lockKey, 100, 3000, TimeUnit.MILLISECONDS)) {
                        try {
                            // 双重检查
                            redisValue = (T) redisTemplate.opsForValue().get(key);
                            if (redisValue != null) {
                                return redisValue;
                            }

                            // 调用 valueLoader 获取数据（数据库查询）
                            T loadedValue = valueLoader.call();

                            if (loadedValue != null) {
                                // 设置随机 TTL 防止缓存雪崩
                                long randomTtl = getRandomTtl(ttlSeconds);
                                putToRedis(key, loadedValue, randomTtl);

                                // 如果是热点key，加入热点key管理
                                if (hotKeyManager.isHotKey(key)) {
                                    hotKeyManager.addHotKey(key, randomTtl);
                                }
                            }
                            return loadedValue;
                        } finally {
                            distributedLock.unlock(lockKey);
                        }
                    } else {
                        // 获取锁失败，短暂等待后重试或返回旧数据
                        Thread.sleep(100);
                        return redisTemplate.opsForValue().get(key);
                    }
                } catch (Exception e) {
                    throw new CacheException("Get cache error", e);
                }
            });

            return value;
        } catch (Exception e) {
            throw new CacheException("Get cache error", e);
        }
    }

    private void putToRedis(String key, Object value, long ttlSeconds) {
        try {
            redisTemplate.opsForValue().set(key, value, ttlSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            // 记录日志，不影响主流程
        }
    }


    /**
     * 生成随机 TTL，为缓存项设置不同的过期时间，以避免缓存雪崩
     * @param baseTtlSeconds 基础 TTL（秒）
     * @return 基础 TTL + 随机偏移（秒）
     */
    private long getRandomTtl(long baseTtlSeconds) {
        Duration range = cacheProperties.getRedis().getRandomTtlRange();

        // randomTtlRange <= 0 直接返回
        if (range.isZero() || range.isNegative()) {
            return baseTtlSeconds;
        }

        // 确保 range 合理（比如不超过 1 小时）
        long maxRangeSeconds = Duration.ofHours(1).getSeconds();
        long rangeSeconds = Math.min(range.getSeconds(), maxRangeSeconds);

        // 生成 [0, rangeMillis] 的随机偏移量
        long randomOffSet = ThreadLocalRandom.current().nextLong(rangeSeconds);

        // 防御溢出
        return Math.addExact(baseTtlSeconds, randomOffSet);
    }

    @Override
    public void put(String key, Object value, long ttlSeconds) {
        try {
            // 设置随机TTL
            long randomTtl = getRandomTtl(ttlSeconds);

            // 更新本地缓存
            localCache.put(key, value);

            // 更新Redis缓存
            putToRedis(key, value, randomTtl);

            // 如果是热点key，更新热点key管理
            if (hotKeyManager.isHotKey(key)) {
                hotKeyManager.addHotKey(key, randomTtl);
            }
        } catch (Exception e) {
            throw new CacheException("Put cache error", e);
        }
    }

    @Override
    public void evict(String key) {
        try {
            localCache.invalidate(key);
            redisTemplate.delete(key);
        } catch (Exception e) {
            throw new CacheException("Evict cache error", e);
        }
    }

    @Override
    public void clear() {
        try {
            localCache.invalidateAll();
            // Redis不建议直接清空，这里只是示例
            // 生产环境应该使用更精细的缓存清理策略
            Set<String> keys = redisTemplate.keys("*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            throw new CacheException("Clear cache error", e);
        }
    }
}