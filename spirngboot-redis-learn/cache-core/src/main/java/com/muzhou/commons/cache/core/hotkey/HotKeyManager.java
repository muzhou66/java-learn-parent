package com.muzhou.commons.cache.core.hotkey;

import java.util.Set;

public interface HotKeyManager {

    /**
     * 访问记录
     */
    void recordAccess(String key);

    boolean isHotKey(String key);

    void addHotKey(String key, long ttlSeconds);

    Set<String> getHotKeys();

    void addListener(HotKeyListener listener);

    void removeListener(HotKeyListener listener);
}
