package org.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.Date;

@Data
@TableName("travel_guide")
@Schema(description = "旅游攻略实体类")
public class TravelGuide {
    @TableId(type = IdType.AUTO)
    @Schema(description = "攻略ID")
    private Long id;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "内容")
    private String content;

    @Schema(description = "作者ID")
    private Long userId;

    @Schema(description = "封面图片")
    private String coverImage;

    @Schema(description = "浏览量")
    private Integer views;

    @Schema(description = "创建时间")
    private Date createTime;

    @Schema(description = "更新时间")
    private Date updateTime;

    @TableField(exist = false)
    @Schema(description = "作者昵称")
    private String userNickname;

    @TableField(exist = false)
    @Schema(description = "作者头像")
    private String userAvatar;
} 