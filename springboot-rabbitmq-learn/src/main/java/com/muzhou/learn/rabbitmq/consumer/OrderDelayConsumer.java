package com.muzhou.learn.rabbitmq.consumer;

import com.muzhou.learn.rabbitmq.config.RabbitMQConfig;
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
 * 延迟订单消息消费者
 */
@Slf4j
@Component
public class OrderDelayConsumer {
    
    @Autowired
    private OrderService orderService;
    
    /**
     * 处理延迟订单消息
     */
    @RabbitListener(queues = RabbitMQConfig.ORDER_DL_QUEUE)
    public void handleDelayOrder(Long orderId, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
        log.info("收到订单延迟消息, 订单ID: {}", orderId);
        
        try {
            // 检查订单状态并取消未支付订单
            orderService.cancelOrder(orderId);
            
            // 手动确认消息
            channel.basicAck(tag, false);
            log.info("订单延迟消息处理完成, 订单ID: {}", orderId);
        } catch (Exception e) {
            log.error("订单延迟消息处理异常, 订单ID: {}, 异常信息: {}", orderId, e.getMessage(), e);
            
            // 处理失败，拒绝消息并重新入队
            channel.basicReject(tag, true);
            
            // 这里可以添加重试次数限制
        }
    }
}