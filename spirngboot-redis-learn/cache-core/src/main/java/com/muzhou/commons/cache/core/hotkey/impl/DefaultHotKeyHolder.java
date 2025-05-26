package com.muzhou.commons.cache.core.hotkey.impl;

import com.muzhou.commons.cache.core.hotkey.HotKeyHolder;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class DefaultHotKeyHolder implements HotKeyHolder {
    private final Set<String> hotKeys = ConcurrentHashMap.newKeySet();

    @Override
    public boolean isHotKey(String key) {
        return hotKeys.contains(key);
    }

    @Override
    public boolean addHotKey(String key) {
        return hotKeys.add(key);
    }

    @Override
    public void refreshHotKeys(Set<String> keys) {
        hotKeys.clear();
        hotKeys.addAll(keys);
    }

    @Override
    public Set<String> getHotKeys() {
        return new HashSet<>(hotKeys);
    }
}