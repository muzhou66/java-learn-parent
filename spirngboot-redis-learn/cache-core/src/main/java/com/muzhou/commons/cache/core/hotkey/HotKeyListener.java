package com.muzhou.commons.cache.core.hotkey;

import java.util.Set;

/**
 * 监听热门关键词变更的回调接口。
 */
public interface HotKeyListener {
    /**
     * 单关键词添加时触发。
     * @param key 新增关键词，非空
     */
    void onHotKeyAdded(String key);

    /**
     * 关键词集合变更时触发（含批量更新/删除）。
     * @param addedKeys   新增关键词集合（空集表示无新增）
     * @param removedKeys 移除关键词集合（空集表示无移除）
     */
    void onHotKeyChanged(Set<String> addedKeys, Set<String> removedKeys);
}