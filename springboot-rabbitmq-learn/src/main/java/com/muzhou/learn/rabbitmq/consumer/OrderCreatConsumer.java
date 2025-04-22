package com.muzhou.learn.rabbitmq.consumer;

import com.muzhou.learn.rabbitmq.config.RabbitMQConfig;
import com.muzhou.learn.rabbitmq.entity.Order;
import com.muzhou.learn.rabbitmq.service.OrderService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 订单创建消息消费者
 */
@Slf4j
@Component
public class OrderCreatConsumer {

    @Autowired
    private OrderService orderService;

    /**
     * 处理订单创建消息
     *
     * @param order   从消息体中反序列化得到的订单对象
     * @param channel RabbitMQ 通道，用于手动确认消息
     * @param tag     消息的投递标签，用于唯一标识消息
     */
    @RabbitListener(queues = RabbitMQConfig.ORDER_QUEUE)
    public void handleOrderCreateMessage(Order order, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag)
            throws IOException {
        log.info("收到订单创建消息, 订单ID: {}", order.getId());

        try {
            // 模拟业务处理
            log.info("处理订单创建逻辑, 订单号: {}", order.getOrderNo());

            // 这里可以添加实际的业务逻辑，如库存预占、优惠券核销等

            // 处理成功，手动确认消息
            channel.basicAck(tag, false);
            log.info("订单创建消息处理完成, 订单ID: {}", order.getId());
        } catch (Exception e) {
            log.error("订单创建消息处理异常, 订单ID: {}, 异常信息: {}", order.getId(), e.getMessage(), e);

            // 处理失败，拒绝消息并不重新入队
            channel.basicReject(tag, false);

            // 这里可以添加重试逻辑或记录错误日志
        }
    }

}
