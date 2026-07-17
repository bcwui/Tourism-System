package org.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("ticket")
@Schema(description = "门票实体类")
public class Ticket {
    @TableId(type = IdType.AUTO)
    @Schema(description = "门票ID")
    private Long id;

    @Schema(description = "景点ID")
    private Long scenicId;

    @Schema(description = "门票名称")
    private String ticketName;

    @Schema(description = "门票价格")
    private BigDecimal price;

    @Schema(description = "优惠价格")
    private BigDecimal discountPrice;

    @Schema(description = "门票类型")
    private String ticketType;

    @Schema(description = "有效期")
    private String validPeriod;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "库存")
    private Integer stock;

    @Schema(description = "状态: 1-可预订, 0-不可预订")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
} 