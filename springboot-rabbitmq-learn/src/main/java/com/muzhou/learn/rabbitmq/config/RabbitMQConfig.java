package com.muzhou.learn.rabbitmq.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
public class RabbitMQConfig {

    // 普通订单相关配置
    public static final String ORDER_QUEUE = "order.queue";
    public static final String ORDER_EXCHANGE = "order.exchange";
    public static final String ORDER_ROUTING_KEY = "order.create";

    // 延迟订单相关配置
    public static final String ORDER_DELAY_QUEUE = "order.delay.queue";
    public static final String ORDER_DELAY_EXCHANGE = "order.delay.exchange";
    public static final String ORDER_DELAY_ROUTING_KEY = "order.delay";

    // 死信队列配置
    public static final String ORDER_DL_EXCHANGE = "order.dl.exchange";
    public static final String ORDER_DL_QUEUE = "order.dl.queue";
    public static final String ORDER_DL_ROUTING_KEY = "order.dl";

    // 创建普通订单队列
    @Bean
    public Queue orderQueue() {
        return new Queue(ORDER_QUEUE, true);
    }

    // 创建普通订单交换机
    @Bean
    public DirectExchange orderExchange() {
        return new DirectExchange(ORDER_EXCHANGE);
    }

    // 绑定普通订单队列和交换机
    @Bean
    public Binding orderBinding(Queue orderQueue, DirectExchange orderExchange) {
        return BindingBuilder.bind(orderQueue)
                .to(orderExchange)
                .with(ORDER_ROUTING_KEY);
    }

    // 创建延迟队列
    @Bean
    public Queue orderDelayQueue() {
        Map<String, Object> args = new HashMap<>();
        // 设置死信交换机
        args.put("x-dead-letter-exchange", ORDER_DL_EXCHANGE);
        // 设置死信路由键
        args.put("x-dead-letter-routing-key", ORDER_DL_ROUTING_KEY);
        // 消息过期时间：30分钟
        args.put("x-message-ttl", 30 * 60 * 1000);
        return new Queue(ORDER_DELAY_QUEUE, true, false, false, args);
    }

    // 创建延迟交换机
    @Bean
    public DirectExchange orderDelayExchange() {
        return new DirectExchange(ORDER_DELAY_EXCHANGE);
    }

    // 绑定延迟队列和交换机
    @Bean
    public Binding orderDelayBinding(Queue orderDelayQueue, DirectExchange orderDelayExchange) {
        return BindingBuilder.bind(orderDelayQueue)
                .to(orderDelayExchange)
                .with(ORDER_DELAY_ROUTING_KEY);
    }

    // 创建死信队列
    @Bean
    public Queue orderDLQueue() {
        return new Queue(ORDER_DL_QUEUE, true);
    }

    // 创建死信交换机
    @Bean
    public DirectExchange orderDLExchange() {
        return new DirectExchange(ORDER_DL_EXCHANGE);
    }

    // 绑定死信队列和交换机
    @Bean
    public Binding orderDLBinding(Queue orderDLQueue, DirectExchange orderDLExchange) {
        return BindingBuilder.bind(orderDLQueue)
                .to(orderDLExchange)
                .with(ORDER_DL_ROUTING_KEY);
    }

    // JSON消息转换器
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // 配置RabbitTemplate
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter jsonMessageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter);

        // 消息发送到交换机确认回调
        template.setConfirmCallback((correlationData, ack, cause) -> {
            String messageId = correlationData != null ? correlationData.getId() : "";
            if (ack) {
                log.info("消息发送到交换机成功, correlationData: {}", correlationData);
                // 更新消息状态为已投递
                messageLogService.updateMessageStatus(messageId, MessageStatus.DELIVERED);
            } else {
                log.error("消息发送到交换机失败, messageId: {}, cause: {}", messageId, cause);
                // 失败处理：重试或记录日志
            }
        });

        // 消息从 [交换机] 发送到 [队列] 失败回调
        template.setReturnsCallback(returnedMessage -> {
            log.error("消息从 Exchange 路由到 Queue 失败: exchange: {}, route: {}, replyCode: {}, replyText: {}",
                    returnedMessage.getExchange(),
                    returnedMessage.getRoutingKey(),
                    returnedMessage.getReplyCode(),
                    returnedMessage.getReplyText());
            // 获取消息 ID
            String messageId = returnedMessage.getMessage().getMessageProperties().getMessageId();
            // 失败处理：重试或记录日志
        });

        // 消息层置为 mandatory，强制退回
        template.setMandatory(true);

        return template;
    }
}
