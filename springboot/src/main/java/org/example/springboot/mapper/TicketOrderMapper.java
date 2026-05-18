package org.example.springboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.springboot.entity.TicketOrder;
 
@Mapper
public interface TicketOrderMapper extends BaseMapper<TicketOrder> {
    // 无需实现任何方法，使用MyBatisPlus提供的方法即可
} 