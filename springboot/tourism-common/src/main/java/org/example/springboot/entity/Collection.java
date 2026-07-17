package org.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Data
@TableName("collection")
@Schema(description = "收藏实体类")
public class Collection {
    @TableId(type = IdType.AUTO)
    @Schema(description = "收藏ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "攻略ID")
    private Long guideId;

    @Schema(description = "创建时间")
    private Date createTime;
    
    @TableField(exist = false)
    @Schema(description = "攻略标题")
    private String guideTitle;
    
    @TableField(exist = false)
    @Schema(description = "攻略封面")
    private String guideCoverImage;
    
    @TableField(exist = false)
    @Schema(description = "攻略浏览量")
    private Integer guideViews;
    
    @TableField(exist = false)
    @Schema(description = "用户名")
    private String username;
    
    @TableField(exist = false)
    @Schema(description = "用户昵称")
    private String userNickname;
    
    @TableField(exist = false)
    @Schema(description = "用户头像")
    private String userAvatar;
} 