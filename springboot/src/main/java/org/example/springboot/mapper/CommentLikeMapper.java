package org.example.springboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.springboot.entity.CommentLike;

@Mapper
public interface CommentLikeMapper extends BaseMapper<CommentLike> {
    // 无需写SQL，使用MyBatis-Plus提供的方法
} 