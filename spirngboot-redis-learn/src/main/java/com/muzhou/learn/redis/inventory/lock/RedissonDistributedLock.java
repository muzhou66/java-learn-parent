package com.muzhou.learn.redis.inventory.lock;

import com.muzhou.learn.common.exception.MuZhouException;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Redisson 分布式锁实现
 */
@Component
@RequiredArgsConstructor
public class RedissonDistributedLock implements DistributedLock {

    private final RedissonClient redissonClient;

    @Override
    public boolean tryLock(String lockKey, long waitTime, long leaseTime) {
        RLock lock = redissonClient.getLock(lockKey);
        try {
            return lock.tryLock(waitTime, leaseTime, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public void unlock(String lockKey) {
        RLock lock = redissonClient.getLock(lockKey);
        if (lock.isLocked() && lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    @Override
    public <T> T executeWithLock(String lockKey, long waitTime, long leaseTime, Supplier<T> supplier) {
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = false;
        try {
            // 尝试获取锁
            locked = lock.tryLock(waitTime, leaseTime, TimeUnit.MILLISECONDS);
            if (locked) {
                return supplier.get();
            }
            throw new MuZhouException("系统繁忙，请稍后重试");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MuZhouException("获取锁被中断");
        } finally {
            // 加锁成功 && 锁为当前线程持有
            if (locked && lock.isHeldByCurrentThread()) {
                // 释放锁
                lock.unlock();
            }
        }
    }
}