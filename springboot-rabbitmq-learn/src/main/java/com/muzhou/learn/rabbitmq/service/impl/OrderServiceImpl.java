package com.muzhou.learn.rabbitmq.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.muzhou.learn.rabbitmq.config.RabbitMQConfig;
import com.muzhou.learn.rabbitmq.dao.OrderMapper;
import com.muzhou.learn.rabbitmq.entity.Order;
import com.muzhou.learn.rabbitmq.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;

@Slf4j
@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    public Order createOrder(Long userId, BigDecimal amount) {
        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setUserId(userId);
        order.setAmount(amount);
        order.setStatus(0); // 待支付
        order.setCreateTime(new Date());

        // 保存订单到数据库
        baseMapper.insert(order);

        // 发送订单创建消息
        sendCreateOrderMessage(order);

        // 发送延迟消息，30分钟后检查订单状态
        sendDelayOrderMessage(order.getId());

        return null;
    }

    @Override
    @Transactional
    public void payOrder(String orderNo) {
        // 查询订单
        Order order = baseMapper.selectOne(Wrappers.lambdaQuery(Order.class)
                .eq(Order::getOrderNo, orderNo));

        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (order.getStatus() != 0) {
            throw new RuntimeException("订单状态不正确");
        }

        // 准备更新对象
        Order updateOrder = new Order();
        updateOrder.setId(order.getId());
        updateOrder.setStatus(1);
        updateOrder.setPayTime(new Date());
        updateOrder.setVersion(order.getVersion());

        // 执行更新
        int result = baseMapper.updateById(updateOrder);

        if (result == 0) {
            // 更新失败，可能已经被其他线程更新
            throw new RuntimeException("更新订单状态失败");
        }

        log.info("订单支付成功, 订单号: {}", orderNo);
    }

    @Override
    @Transactional
    public void cancelOrder(Long orderId) {
        // 查询订单
        Order order = baseMapper.selectById(orderId);

        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (order.getStatus() != 0) {
            log.info("订单已支付或已取消, 无需处理, 订单ID: {}", orderId);
            return;
        }

        // 准备更新对象
        Order updateOrder = new Order();
        updateOrder.setId(orderId);
        updateOrder.setStatus(2);
        updateOrder.setCancelTime(new Date());
        updateOrder.setVersion(order.getVersion());

        // 执行更新
        int result = baseMapper.updateById(updateOrder);
        if (result == 0) {
            log.info("订单状态已变更, 取消订单失败, 订单ID: {}", orderId);
            return;
        }

        log.info("订单取消成功, 订单ID: {}", orderId);
    }

    @Override
    public Order getOrderByNo(String orderNo) {
        return baseMapper.selectOne(Wrappers.lambdaQuery(Order.class)
                .eq(Order::getOrderNo, orderNo));
    }

    /**
     * 发送订单创建消息
     */
    private void sendCreateOrderMessage(Order order) {
        try {
            // 关联消息的唯一标识
            CorrelationData correlationData = new CorrelationData(order.getOrderNo());
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.ORDER_EXCHANGE,
                    RabbitMQConfig.ORDER_ROUTING_KEY,
                    order,
                    correlationData
            );
            log.info("发送订单创建消息成功, 订单ID: {}", order.getId());
        } catch (Exception e) {
            log.error("发送订单创建消息异常, 订单ID: {}", order.getId(), e);
            // 这里可以添加重试逻辑
        }
    }

    /**
     * 发送延迟订单消息
     */
    private void sendDelayOrderMessage(Long orderId) {
        try {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.ORDER_DELAY_EXCHANGE,
                    RabbitMQConfig.ORDER_DELAY_ROUTING_KEY,
                    orderId,
                    message -> {
                        // 设置消息持久化
                        message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                        return message;
                    }
            );
            log.info("发送订单延迟消息成功, 订单ID: {}", orderId);
        } catch (Exception e) {
            log.error("发送订单延迟消息异常, 订单ID: {}", orderId, e);
            // 这里可以添加重试逻辑
        }
    }

    /**
     * 生成订单号
     */
    private String generateOrderNo() {
        // 实际项目中可以使用更复杂的订单号生成规则
        return "ORD" + System.currentTimeMillis() + (int) (Math.random() * 1000);
    }
}
