package org.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("comment")
@Schema(description = "评论实体类")
public class Comment {
    @TableId(type = IdType.AUTO)
    @Schema(description = "评论ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "景点ID")
    private Long scenicId;

    @Schema(description = "评论内容")
    private String content;

    @Schema(description = "评分(1-5)")
    private Integer rating;

    @Schema(description = "点赞数")
    private Integer likes;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "用户昵称")
    @TableField(exist = false)
    private String userNickname;

    @Schema(description = "用户头像")
    @TableField(exist = false)
    private String userAvatar;
    
    @Schema(description = "当前用户是否点赞")
    @TableField(exist = false)
    private Boolean liked = false;
    
    @Schema(description = "景点名称")
    @TableField(exist = false)
    private String scenicName;
} 