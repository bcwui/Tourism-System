package org.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("seckill_activity")
@Schema(description = "秒杀活动实体")
public class SeckillActivity {
    @TableId(type = IdType.AUTO)
    @Schema(description = "活动ID")
    private Long id;

    @Schema(description = "门票ID")
    private Long ticketId;

    @Schema(description = "秒杀价格")
    private BigDecimal seckillPrice;

    @Schema(description = "秒杀库存")
    private Integer seckillStock;

    @Schema(description = "每人限购数量")
    private Integer limitPerUser;

    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    @Schema(description = "状态: 0-未开始, 1-进行中, 2-已结束")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
