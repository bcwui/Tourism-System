package org.example.springboot.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单消息DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderMessageDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 订单ID
     */
    private Long orderId;
    
    /**
     * 订单编号
     */
    private String orderNo;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 用户邮箱
     */
    private String userEmail;
    
    /**
     * 用户手机号
     */
    private String userPhone;
    
    /**
     * 订单状态：0-待支付，1-已支付，2-已取消，3-已退款，4-已完成
     */
    private Integer status;
    
    /**
     * 订单金额
     */
    private BigDecimal amount;
    
    /**
     * 订单类型：ticket-门票订单
     */
    private String orderType;
    
    /**
     * 产品ID
     */
    private Long productId;
    
    /**
     * 产品名称
     */
    private String productName;
    
    /**
     * 产品数量
     */
    private Integer quantity;
    
    /**
     * 订单创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 支付时间
     */
    private LocalDateTime paymentTime;
    
    /**
     * 事件类型: create-创建订单, pay-支付订单, cancel-取消订单, refund-退款订单, complete-完成订单
     */
    private String eventType;
    
    /**
     * 额外信息
     */
    private String extraInfo;
} 