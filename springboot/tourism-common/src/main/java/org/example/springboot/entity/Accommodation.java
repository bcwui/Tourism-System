package org.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("accommodation")
@Schema(description = "住宿实体类")
public class Accommodation {
    
    @TableId(type = IdType.AUTO)
    @Schema(description = "住宿ID")
    private Long id;
    
    @Schema(description = "住宿名称")
    private String name;
    
    @Schema(description = "住宿类型")
    private String type;
    
    @Schema(description = "住宿地址")
    private String address;
    
    @Schema(description = "关联景点ID")
    private Long scenicId;
    
    @Schema(description = "描述")
    private String description;
    
    @Schema(description = "联系电话")
    private String contactPhone;
    
    @Schema(description = "价格区间")
    private String priceRange;
    
    @Schema(description = "评分")
    private BigDecimal starLevel;
    
    @Schema(description = "图片URL")
    private String imageUrl;
    
    @Schema(description = "特色服务")
    private String features;
    
    @Schema(description = "距景点距离")
    private String distance;
    
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
    
    @TableField(exist = false)
    @Schema(description = "关联景点名称")
    private String scenicName;
    
    @TableField(exist = false)
    @Schema(description = "评价数量")
    private Long reviewCount;
} 