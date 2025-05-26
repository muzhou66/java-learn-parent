package com.muzhou.learn.redis.inventory.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_product_inventory")
public class ProductInventory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String productCode;
    private String productName;
    // 总库存
    private Integer totalInventory;
    // 可用库存
    private Integer availableInventory;
    // 锁定库存
    private Integer lockedInventory;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
