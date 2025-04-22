package com.muzhou.learn.rabbitmq.service;

/**
 * 消息幂等性处理
 * 确保即使消息被重复消费，业务逻辑也只会执行一次
 */
public interface IdempotentOrderService {

}
