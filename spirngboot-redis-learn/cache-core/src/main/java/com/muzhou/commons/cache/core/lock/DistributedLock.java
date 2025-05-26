package com.muzhou.commons.cache.core.lock;

import java.util.concurrent.TimeUnit;

public interface DistributedLock {

    /**
     * 尝试获取指定 key 的锁
     *
     * @param waitTime 尝试获取锁的最大等待时间
     * @param leaseTime 锁自动释放前的最大持有时间
     * @param unit 等待时间的单位
     */
    boolean tryLock(String key, long waitTime, long leaseTime, TimeUnit unit) throws InterruptedException;

    void unlock(String key);

}
