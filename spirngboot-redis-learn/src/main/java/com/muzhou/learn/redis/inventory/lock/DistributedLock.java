package com.muzhou.learn.redis.inventory.lock;

import java.util.function.Supplier;

public interface DistributedLock {

    /**
     * 获取锁
     * @param lockKey 锁key
     * @param waitTime 最大等待时间(毫秒)
     * @param leaseTime 锁持有时间(毫秒)
     * @return 是否获取成功
     */
    boolean tryLock(String lockKey, long waitTime, long leaseTime);

    /**
     * 释放锁
     * @param lockKey 锁key
     */
    void unlock(String lockKey);

    /**
     * 执行带锁的业务逻辑
     */
    <T> T executeWithLock(String lockKey, long waitTime, long leaseTime, Supplier<T> supplier);
}