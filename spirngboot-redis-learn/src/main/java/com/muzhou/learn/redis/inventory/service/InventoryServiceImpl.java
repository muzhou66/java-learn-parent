package com.muzhou.learn.redis.inventory.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.muzhou.learn.common.exception.MuZhouException;
import com.muzhou.learn.redis.inventory.entity.ProductInventory;
import com.muzhou.learn.redis.inventory.lock.DistributedLock;
import com.muzhou.learn.redis.inventory.mapper.ProductInventoryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryServiceImpl implements InventoryService {
    @Autowired
    private ProductInventoryMapper inventoryMapper;
    @Autowired
    private DistributedLock distributedLock;

    private static final String INVENTORY_LOCK_PREFIX = "inventory:lock:";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deductInventory(String productCode, int quantity) {
        
        String lockKey = INVENTORY_LOCK_PREFIX + productCode;

        return distributedLock.executeWithLock(lockKey, 1000, 3000, () -> {
            // 1. 检查库存是否充足
            ProductInventory inventory = inventoryMapper.selectOne(
                    new LambdaQueryWrapper<ProductInventory>()
                            .eq(ProductInventory::getProductCode, productCode)
                            .select(ProductInventory::getAvailableInventory)
            );

            if (inventory == null || inventory.getAvailableInventory() < quantity) {
                throw new MuZhouException("库存不足");
            }

            // 2. 扣减可用库存，增加锁定库存
            int updated = inventoryMapper.deductInventory(productCode, quantity);
            if (updated <= 0) {
                throw new MuZhouException("库存扣减失败");
            }

            // 3. 模拟业务处理（实际项目中可能是创建订单等操作）
            try {
                Thread.sleep(100); // 模拟业务处理耗时
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new MuZhouException("处理中断");
            }

            // 4. 确认扣减（将锁定库存从总库存中扣除）
            updated = inventoryMapper.confirmDeduction(productCode, quantity);
            if (updated <= 0) {
                throw new MuZhouException("库存确认失败");
            }

            return true;
        });
    }

    @Override
    public ProductInventory getInventory(String productCode) {
        return inventoryMapper.selectOne(
                new LambdaQueryWrapper<ProductInventory>()
                        .eq(ProductInventory::getProductCode, productCode)
        );
    }
}