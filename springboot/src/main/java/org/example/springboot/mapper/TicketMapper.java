package org.example.springboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.springboot.entity.Ticket;
 
@Mapper
public interface TicketMapper extends BaseMapper<Ticket> {
    // 无需实现任何方法，使用MyBatisPlus提供的方法即可
} 