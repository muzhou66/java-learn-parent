package com.muzhou.learn.redis.inventory.controller;

import com.muzhou.learn.common.response.ApiResponse;
import com.muzhou.learn.redis.inventory.entity.ProductInventory;
import com.muzhou.learn.redis.inventory.service.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

    @Autowired
    private InventoryService inventoryService;

    @PostMapping("/deduct")
    public ApiResponse<Boolean> deductInventory(
            @RequestParam String productCode,
            @RequestParam(defaultValue = "1") int quantity) {
        boolean success = inventoryService.deductInventory(productCode, quantity);
        return ApiResponse.success(success);
    }

    @GetMapping("/{productCode}")
    public ApiResponse<ProductInventory> getInventory(
            @PathVariable String productCode) {
        return ApiResponse.success(inventoryService.getInventory(productCode));
    }
}