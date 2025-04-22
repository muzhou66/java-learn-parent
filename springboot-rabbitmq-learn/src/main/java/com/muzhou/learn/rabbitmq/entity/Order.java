package com.muzhou.learn.rabbitmq.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName("order")
public class Order {
    @TableId
    private Long id;

    private String orderNo;          // 订单编号
    private Long userId;             // 用户ID
    private BigDecimal amount;       // 订单金额
    private Integer status;          // 订单状态: 0-待支付,1-已支付,2-已取消
    private Date createTime;         // 创建时间
    private Date payTime;            // 支付时间
    private Date cancelTime;         // 取消时间

    @Version
    private Integer version;         // 乐观锁版本号
}
