package org.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("ticket_order")
@Schema(description = "门票订单实体类")
public class TicketOrder {
    @TableId(type = IdType.AUTO)
    @Schema(description = "订单ID")
    private Long id;

    @Schema(description = "订单编号")
    private String orderNo;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "门票ID")
    private Long ticketId;

    @Schema(description = "购买数量")
    private Integer quantity;

    @Schema(description = "游客姓名")
    private String visitorName;

    @Schema(description = "游客手机号")
    private String visitorPhone;

    @Schema(description = "身份证号")
    private String idCard;

    @Schema(description = "游玩日期")
    private LocalDate visitDate;

    @Schema(description = "订单总金额")
    private BigDecimal totalAmount;

    @Schema(description = "订单状态: 0-待支付, 1-已支付, 2-已取消, 3-已退款, 4-已完成")
    private Integer status;

    @Schema(description = "支付时间")
    private LocalDateTime paymentTime;

    @Schema(description = "支付方式")
    private String paymentMethod;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
    
    // 非数据库字段，用于前端显示
    @TableField(exist = false)
    @Schema(description = "门票名称")
    private String ticketName;
    
    @TableField(exist = false)
    @Schema(description = "景点名称")
    private String scenicName;
    
    @TableField(exist = false)
    @Schema(description = "用户名")
    private String username;
} 