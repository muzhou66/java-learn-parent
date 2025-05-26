package com.muzhou.commons.cache.core.hotkey;

import java.util.Set;

/**
 * 热点 Key 持有器接口
 */
public interface HotKeyHolder {
    boolean isHotKey(String key);

    boolean addHotKey(String key);

    void refreshHotKeys(Set<String> keys);

    Set<String> getHotKeys();
}