package com.muzhou.learn.rabbitmq.controller;

import com.muzhou.learn.rabbitmq.entity.Order;
import com.muzhou.learn.rabbitmq.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@Slf4j
@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 创建订单
     */
    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestParam Long userId, @RequestParam BigDecimal amount) {
        Order order = orderService.createOrder(userId, amount);
        return ResponseEntity.ok(order);
    }

    /**
     * 支付订单
     */
    @PostMapping("/{orderNo}/pay")
    public ResponseEntity<String> payOrder(@PathVariable String orderNo) {
        orderService.payOrder(orderNo);
        return ResponseEntity.ok("订单支付成功");
    }

    /**
     * 查询订单
     */
    @GetMapping("/{orderNo}")
    public ResponseEntity<Order> getOrder(@PathVariable String orderNo) {
        Order order = orderService.getOrderByNo(orderNo);
        return ResponseEntity.ok(order);
    }
}
