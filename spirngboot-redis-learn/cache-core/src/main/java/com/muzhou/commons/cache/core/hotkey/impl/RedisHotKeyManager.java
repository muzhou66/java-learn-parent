package com.muzhou.commons.cache.core.hotkey.impl;

import com.muzhou.commons.cache.autoconfigure.properties.HotKeyProperties;
import com.muzhou.commons.cache.core.hotkey.HotKeyHolder;
import com.muzhou.commons.cache.core.hotkey.HotKeyListener;
import com.muzhou.commons.cache.core.hotkey.HotKeyManager;
import io.micrometer.core.instrument.util.NamedThreadFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import javax.annotation.PreDestroy;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * 基于Redis的热点Key管理器实现
 */
@Slf4j
public class RedisHotKeyManager implements HotKeyManager {

    private static final String HOT_KEY_PREFIX = "multilevel:hotkey:";
    private static final String HOT_KEY_STATS_ZSET = "multilevel:hotkey:stats";
    private static final String HOT_KEY_PRELOAD_LOCK = "multilevel:hotkey:preload:lock";

    private final RedisTemplate<String, Object> redisTemplate;
    private final HotKeyProperties properties;
    private final HotKeyHolder hotKeyHolder;
    private final ScheduledExecutorService scheduler;
    private final List<HotKeyListener> listeners = new CopyOnWriteArrayList<>();
    private final AtomicBoolean running = new AtomicBoolean(true);

    private static final int MAX_RETRIES = 3;

    public RedisHotKeyManager(RedisTemplate<String, Object> redisTemplate,
                              HotKeyProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
        this.hotKeyHolder = new DefaultHotKeyHolder();

        this.scheduler = Executors.newScheduledThreadPool(2,
                new NamedThreadFactory("hotkey-manager-"));

        init();
    }

    private void init() {
        // 初始加载热点key
        loadInitialHotKeys();

        // 定时分析热点 key
        scheduler.scheduleWithFixedDelay(this::analyzeHotKeys,
                properties.getInitialDelay(),
                properties.getAnalyzeInterval(),
                TimeUnit.SECONDS);

        // 热点 key 预加载
        scheduler.scheduleWithFixedDelay(this::preloadHotKeys,
                properties.getInitialDelay() + 5, // 错开分析时间
                properties.getPreloadInterval(),
                TimeUnit.SECONDS);
    }

    private void loadInitialHotKeys() {
        try {
            Set<String> keys = redisTemplate.keys(HOT_KEY_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                Set<String> hotKeys = keys.stream()
                        .map(key -> key.substring(HOT_KEY_PREFIX.length()))
                        .collect(Collectors.toSet());
                hotKeyHolder.refreshHotKeys(hotKeys);

                // 记录日志
                if (properties.isDebug()) {
                    System.out.println("[HotKey] Load initial hot keys: " + hotKeys);
                }
            }
        } catch (Exception e) {
            // 记录错误日志
            System.err.println("[HotKey] Load initial hot keys failed: " + e.getMessage());
        }
    }

    /**
     * 访问统计
     */
    @Override
    public void recordAccess(String key) {
        // 参数校验
        if (!running.get() || key == null || key.isEmpty()) {
            return;
        }

        int retries = 0;
        while (retries < MAX_RETRIES) {
            try {
                // ZSet [member] 访问 Key [score] 访问频次
                redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                    connection.zSetCommands().zIncrBy(HOT_KEY_STATS_ZSET.getBytes(), 1, key.getBytes());
                    return null;
                });
                break;
            } catch (Exception e) {
                retries++;
                if (retries >= MAX_RETRIES) {
                    // 记录错误日志
                    System.err.println("[HotKey] Record access failed after " + retries + " retries for key: " + key);
                    e.printStackTrace();

                    // 考虑添加降级策略，如本地缓存统计
                } else {
                    // 短暂休眠后重试
                    try {
                        Thread.sleep(100L * retries);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
    }

    @Override
    public boolean isHotKey(String key) {
        return hotKeyHolder.isHotKey(key);
    }

    @Override
    public void addHotKey(String key, long ttlSeconds) {
        if (!running.get() || key == null || key.isEmpty()) {
            return;
        }

        try {
            String hotKey = HOT_KEY_PREFIX + key;
            redisTemplate.opsForValue().set(hotKey, 1, ttlSeconds, TimeUnit.SECONDS);
            boolean added = hotKeyHolder.addHotKey(key);

            if (added) {
                // 通知监听器
                notifyHotKeyAdded(key);

                if (properties.isDebug()) {
                    System.out.println("[HotKey] New hot key added: " + key);
                }
            }
        } catch (Exception e) {
            // 记录错误日志
            System.err.println("[HotKey] Add hot key failed: " + key + ", error: " + e.getMessage());
        }
    }

    /**
     * 统计分析
     */
    private void analyzeHotKeys() {
        if (!running.get()) {
            return;
        }

        try {
            // 获取 TopN 的热点 Key
            Set<ZSetOperations.TypedTuple<Object>> topKeys = redisTemplate.opsForZSet()
                    .reverseRangeWithScores(HOT_KEY_STATS_ZSET, 0, properties.getTopN() - 1);

            if (topKeys != null && !topKeys.isEmpty()) {
                Set<String> newHotKeys = new HashSet<>();
                for (ZSetOperations.TypedTuple<Object> tuple : topKeys) {
                    String key = (String) tuple.getValue();
                    Double score = tuple.getScore();

                    if (key != null && score != null && score >= properties.getHotThreshold()) {
                        newHotKeys.add(key);
                    }
                }

                // 更新热点 key 集合
                hotKeyHolder.refreshHotKeys(newHotKeys);

                // 比较新旧热点key变化，通知监听器
                Set<String> oldHotKeys = hotKeyHolder.getHotKeys();
                Set<String> addedKeys = new HashSet<>(newHotKeys);
                addedKeys.removeAll(oldHotKeys);

                Set<String> removedKeys = new HashSet<>(oldHotKeys);
                removedKeys.removeAll(newHotKeys);

                notifyHotKeyChanged(addedKeys, removedKeys);

                if (properties.isDebug()) {
                    log.debug("[HotKey] Hot key analysis result - " +
                            "Total: " + newHotKeys.size() +
                            ", Added: " + addedKeys +
                            ", Removed: " + removedKeys);
                }
            }
        } catch (Exception e) {
            // 记录错误日志
            System.err.println("[HotKey] Analyze hot keys failed: " + e.getMessage());
        } finally {
            try {
                // 重置统计
                redisTemplate.delete(HOT_KEY_STATS_ZSET);
            } catch (Exception e) {
                // 记录错误日志
                System.err.println("[HotKey] Clear stats failed: " + e.getMessage());
            }
        }
    }

    /**
     * 预加载
     */
    private void preloadHotKeys() {
        if (!running.get()) {
            return;
        }

        // 获取分布式锁，防止多个实例同时预加载
        String lockKey = HOT_KEY_PRELOAD_LOCK;
        boolean locked = false;

        try {
            // todo:redission
            locked = redisTemplate.opsForValue().setIfAbsent(
                    lockKey,
                    "1",
                    properties.getPreloadInterval() / 2,
                    TimeUnit.SECONDS);

            if (!locked) {
                return;
            }

            Set<String> hotKeys = hotKeyHolder.getHotKeys();
            if (hotKeys.isEmpty()) {
                return;
            }

            if (properties.isDebug()) {
                System.out.println("[HotKey] Start preloading hot keys: " + hotKeys.size());
            }

            // 使用并行流提高预加载效率
            hotKeys.parallelStream().forEach(key -> {
                try {
                    // todo:触发缓存加载
                    Object value = redisTemplate.opsForValue().get(key);

                    if (value == null && properties.isDebug()) {
                        System.out.println("[HotKey] Preload found null value for key: " + key);
                    }
                } catch (Exception e) {
                    // 记录错误日志
                    log.error("[HotKey] Preload failed for key: " + key + ", error: " + e.getMessage());
                }
            });

        } catch (Exception e) {
            // 记录错误日志
            System.err.println("[HotKey] Preload hot keys failed: " + e.getMessage());
        } finally {
            if (locked) {
                try {
                    redisTemplate.delete(lockKey);
                } catch (Exception e) {
                    // 记录错误日志
                    System.err.println("[HotKey] Release preload lock failed: " + e.getMessage());
                }
            }
        }
    }

    private void notifyHotKeyAdded(String key) {
        for (HotKeyListener listener : listeners) {
            try {
                listener.onHotKeyAdded(key);
            } catch (Exception e) {
                // 记录错误日志
                System.err.println("[HotKey] Notify hot key added failed: " + e.getMessage());
            }
        }
    }

    private void notifyHotKeyChanged(Set<String> addedKeys, Set<String> removedKeys) {
        if ((addedKeys == null || addedKeys.isEmpty()) &&
                (removedKeys == null || removedKeys.isEmpty())) {
            return;
        }

        for (HotKeyListener listener : listeners) {
            try {
                listener.onHotKeyChanged(addedKeys, removedKeys);
            } catch (Exception e) {
                // 记录错误日志
                System.err.println("[HotKey] Notify hot key changed failed: " + e.getMessage());
            }
        }
    }

    @Override
    public void addListener(HotKeyListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeListener(HotKeyListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    @Override
    public Set<String> getHotKeys() {
        return hotKeyHolder.getHotKeys();
    }

    @PreDestroy
    public void destroy() {
        running.set(false);
        scheduler.shutdownNow();
        listeners.clear();
    }
}