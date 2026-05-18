package org.example.springboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.springboot.entity.AccommodationReview;

@Mapper
public interface AccommodationReviewMapper extends BaseMapper<AccommodationReview> {
    // 使用MyBatis-Plus提供的方法，无需额外定义SQL
} 