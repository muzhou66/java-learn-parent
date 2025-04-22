package com.muzhou.learn.rabbitmq.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.muzhou.learn.rabbitmq.entity.Order;

import java.math.BigDecimal;

public interface OrderService extends IService<Order> {

    /**
     * 创建订单
     */
    Order createOrder(Long userId, BigDecimal amount);

    /**
     * 支付订单
     */
    void payOrder(String orderNo);

    /**
     * 取消订单
     */
    void cancelOrder(Long orderId);

    /**
     * 查询订单
     */
    Order getOrderByNo(String orderNo);
}
