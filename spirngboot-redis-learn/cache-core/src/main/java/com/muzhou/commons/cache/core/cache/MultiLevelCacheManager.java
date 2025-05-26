package com.muzhou.commons.cache.core.cache;

import java.util.concurrent.Callable;

public interface MultiLevelCacheManager {

    <T> T get(String key, Callable<T> valueLoader, long ttlSeconds);

    void put(String key, Object value, long ttlSeconds);

    void evict(String key);

    void clear();
}
