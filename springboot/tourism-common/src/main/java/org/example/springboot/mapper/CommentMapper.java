package org.example.springboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.example.springboot.entity.Comment;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {

    @Update("UPDATE comment SET likes = likes + 1 WHERE id = #{commentId}")
    int incrementLikes(@Param("commentId") Long commentId);

    @Update("UPDATE comment SET likes = GREATEST(likes - 1, 0) WHERE id = #{commentId} AND likes > 0")
    int decrementLikes(@Param("commentId") Long commentId);
}