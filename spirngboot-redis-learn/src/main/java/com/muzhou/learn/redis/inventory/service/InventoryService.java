package com.muzhou.learn.redis.inventory.service;

import com.muzhou.learn.redis.inventory.entity.ProductInventory;

public interface InventoryService {

    /**
     * 扣减库存
     * @param productCode 商品码
     * @param quantity 扣减数量
     * @return 是否扣减成功
     */
    boolean deductInventory(String productCode, int quantity);

    /**
     * 获取库存
     * @param productCode 商品码
     * @return 商品库存信息
     */
    ProductInventory getInventory(String productCode);
}
