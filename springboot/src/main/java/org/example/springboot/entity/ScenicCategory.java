package org.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.List;

@Data
@ToString(exclude = "children")
@EqualsAndHashCode(exclude = "children")
@TableName("scenic_category")
@Schema(description = "景点分类实体类")
public class ScenicCategory {
    
    @TableId(type = IdType.AUTO)
    @Schema(description = "分类ID")
    private Long id;
    
    @Schema(description = "分类名称")
    private String name;
    
    @Schema(description = "分类描述")
    private String description;
    
    @Schema(description = "父分类ID")
    private Long parentId;
    
    @Schema(description = "排序序号")
    private Integer sortOrder;
    
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
    
    @TableField(exist = false)
    @Schema(description = "子分类列表")
    private List<ScenicCategory> children;
} 