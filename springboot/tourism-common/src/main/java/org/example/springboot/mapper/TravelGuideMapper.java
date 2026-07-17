package org.example.springboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.example.springboot.entity.TravelGuide;
import org.apache.ibatis.annotations.Mapper;
 
@Mapper
public interface TravelGuideMapper extends BaseMapper<TravelGuide> {
    // 无需写SQL
} 