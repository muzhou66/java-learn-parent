package com.muzhou.learn.redis.inventory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.muzhou.learn.redis.inventory.entity.ProductInventory;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface ProductInventoryMapper extends BaseMapper<ProductInventory> {
    
    @Update("UPDATE t_product_inventory SET " +
            "available_inventory = available_inventory - #{quantity}, " +
            "locked_inventory = locked_inventory + #{quantity} " +
            "WHERE product_code = #{productCode} AND available_inventory >= #{quantity}")
    int deductInventory(@Param("productCode") String productCode,
                       @Param("quantity") int quantity);
    
    @Update("UPDATE t_product_inventory SET " +
            "locked_inventory = locked_inventory - #{quantity}, " +
            "total_inventory = total_inventory - #{quantity} " +
            "WHERE product_code = #{productCode} AND locked_inventory >= #{quantity}")
    int confirmDeduction(@Param("productCode") String productCode,
                        @Param("quantity") int quantity);
    
    @Update("UPDATE t_product_inventory SET " +
            "available_inventory = available_inventory + #{quantity}, " +
            "locked_inventory = locked_inventory - #{quantity} " +
            "WHERE product_code = #{productCode} AND locked_inventory >= #{quantity}")
    int cancelDeduction(@Param("productCode") String productCode,
                       @Param("quantity") int quantity);
}