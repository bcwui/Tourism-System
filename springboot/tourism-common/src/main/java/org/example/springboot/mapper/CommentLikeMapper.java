package org.example.springboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Update;
import org.example.springboot.entity.CommentLike;

@Mapper
public interface CommentLikeMapper extends BaseMapper<CommentLike> {

    @Insert("INSERT IGNORE INTO comment_like (user_id, comment_id, create_time) VALUES (#{userId}, #{commentId}, NOW())")
    int insertIgnore(@Param("userId") Long userId, @Param("commentId") Long commentId);

    @Update("UPDATE comment SET likes = likes + 1 WHERE id = #{commentId}")
    int incrementLikes(@Param("commentId") Long commentId);

    @Update("UPDATE comment SET likes = GREATEST(likes - 1, 0) WHERE id = #{commentId} AND likes > 0")
    int decrementLikes(@Param("commentId") Long commentId);
}