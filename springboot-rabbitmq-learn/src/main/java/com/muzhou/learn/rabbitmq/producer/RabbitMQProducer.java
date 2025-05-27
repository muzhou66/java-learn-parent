package com.muzhou.learn.rabbitmq.producer;

import com.muzhou.learn.rabbitmq.config.MessageStatus;
import com.muzhou.learn.rabbitmq.config.RabbitMQConfig;
import com.muzhou.learn.rabbitmq.entity.Order;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 生产者
 */
@Slf4j
@Service
public class RabbitMQProducer {
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private MessageLogService messageLogService;

    /**
     * 发送订单消息
     */
    public void sendOrderMessage(Order order) {
        // 生成全局唯一消息 ID
        String messageId = UUID.randomUUID().toString();

        // 构建消息
        Message message = MessageHelper.buildMessage(order, messageId);

        // 消息落库
        MessageLog messageLog = MessageLog.builder()
                .messageId(messageId)
                .message(JsonUtil.toJson(order))
                .status(MessageStatus.SENDING)
                .exchange(RabbitMQConfig.ORDER_EXCHANGE)
                .routingKey(RabbitMQConfig.ORDER_ROUTING_KEY)
                .tryCount(0)
                .nextRetryTime(LocalDateTime.now().plusMinutes(1))
                .createTime(LocalDateTime.now())
                .build();
        messageLogService.save(messageLog);

        // 发送消息
        CorrelationData correlationData = new CorrelationData(messageId);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.ORDER_EXCHANGE,
                RabbitMQConfig.ORDER_ROUTING_KEY,
                message,
                correlationData
        );
    }


}
