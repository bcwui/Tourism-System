package org.example.springboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.springboot.entity.ScenicCategory;

@Mapper
public interface ScenicCategoryMapper extends BaseMapper<ScenicCategory> {
    // 继承BaseMapper，无需编写额外方法
} 