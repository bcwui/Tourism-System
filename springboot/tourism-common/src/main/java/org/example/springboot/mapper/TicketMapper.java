package org.example.springboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.example.springboot.entity.Ticket;

@Mapper
public interface TicketMapper extends BaseMapper<Ticket> {

    @Update("UPDATE ticket SET stock = stock - #{quantity} WHERE id = #{ticketId} AND stock >= #{quantity}")
    int deductStock(@Param("ticketId") Long ticketId, @Param("quantity") Integer quantity);

    @Update("UPDATE ticket SET stock = stock + #{quantity} WHERE id = #{ticketId}")
    int restoreStock(@Param("ticketId") Long ticketId, @Param("quantity") Integer quantity);
}