package org.example.springboot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Date;

@Data
@TableName("comment_like")
@Schema(description = "评论点赞关系实体类")
public class CommentLike {
    @TableId(type = IdType.AUTO)
    @Schema(description = "点赞ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "评论ID")
    private Long commentId;

    @Schema(description = "创建时间")
    private Date createTime;
} 