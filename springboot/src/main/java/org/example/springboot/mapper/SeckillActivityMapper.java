package org.example.springboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.example.springboot.entity.SeckillActivity;

@Mapper
public interface SeckillActivityMapper extends BaseMapper<SeckillActivity> {

    @Update("UPDATE seckill_activity SET seckill_stock = seckill_stock - #{quantity} WHERE id = #{activityId} AND seckill_stock >= #{quantity}")
    int deductStock(@Param("activityId") Long activityId, @Param("quantity") Integer quantity);
}
