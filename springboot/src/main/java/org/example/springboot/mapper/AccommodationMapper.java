package org.example.springboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.springboot.entity.Accommodation;

@Mapper
public interface AccommodationMapper extends BaseMapper<Accommodation> {
    // 使用MyBatis-Plus提供的方法，无需额外定义SQL
} 