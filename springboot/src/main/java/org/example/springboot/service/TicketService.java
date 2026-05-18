package org.example.springboot.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.example.springboot.entity.ScenicSpot;
import org.example.springboot.entity.Ticket;
import org.example.springboot.exception.ServiceException;
import org.example.springboot.mapper.ScenicSpotMapper;
import org.example.springboot.mapper.TicketMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketService {

    @Resource
    private TicketMapper ticketMapper;
    
    @Resource
    private ScenicSpotMapper scenicSpotMapper;

    /**
     * 分页查询门票
     */
    public Page<Ticket> getTicketsByPage(String ticketName, String ticketType, Long scenicId, Integer currentPage, Integer size) {
        LambdaQueryWrapper<Ticket> queryWrapper = new LambdaQueryWrapper<>();
        
        // 添加查询条件
        if (StringUtils.isNotBlank(ticketName)) {
            queryWrapper.like(Ticket::getTicketName, ticketName);
        }
        if (StringUtils.isNotBlank(ticketType)) {
            queryWrapper.eq(Ticket::getTicketType, ticketType);
        }
        if (scenicId != null) {
            queryWrapper.eq(Ticket::getScenicId, scenicId);
        }
        
        // 只查询可预订的门票
        queryWrapper.eq(Ticket::getStatus, 1);
        
        // 按创建时间降序排序
        queryWrapper.orderByDesc(Ticket::getCreateTime);
        
        return ticketMapper.selectPage(new Page<>(currentPage, size), queryWrapper);
    }
    
    /**
     * 根据ID获取门票详情
     */
    public Ticket getTicketById(Long id) {
        Ticket ticket = ticketMapper.selectById(id);
        if (ticket == null) {
            throw new ServiceException("门票不存在");
        }
        return ticket;
    }
    
    /**
     * 新增门票
     */
    @Transactional
    public void addTicket(Ticket ticket) {
        // 检查景点是否存在
        ScenicSpot scenicSpot = scenicSpotMapper.selectById(ticket.getScenicId());
        if (scenicSpot == null) {
            throw new ServiceException("关联的景点不存在");
        }
        
        // 设置默认状态为可预订
        if (ticket.getStatus() == null) {
            ticket.setStatus(1);
        }
        
        ticketMapper.insert(ticket);
    }
    
    /**
     * 更新门票信息
     */
    @Transactional
    public void updateTicket(Ticket ticket) {
        Ticket existTicket = ticketMapper.selectById(ticket.getId());
        if (existTicket == null) {
            throw new ServiceException("门票不存在");
        }
        
        // 如果景点ID有变更，需要检查新景点是否存在
        if (ticket.getScenicId() != null && !ticket.getScenicId().equals(existTicket.getScenicId())) {
            ScenicSpot scenicSpot = scenicSpotMapper.selectById(ticket.getScenicId());
            if (scenicSpot == null) {
                throw new ServiceException("关联的景点不存在");
            }
        }
        
        ticketMapper.updateById(ticket);
    }
    
    /**
     * 删除门票
     */
    @Transactional
    public void deleteTicket(Long id) {
        Ticket ticket = ticketMapper.selectById(id);
        if (ticket == null) {
            throw new ServiceException("门票不存在");
        }
        
        ticketMapper.deleteById(id);
    }
    
    /**
     * 获取某个景点的所有可预订门票
     */
    public Page<Ticket> getTicketsByScenicId(Long scenicId, Integer currentPage, Integer size) {
        LambdaQueryWrapper<Ticket> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Ticket::getScenicId, scenicId);
        queryWrapper.eq(Ticket::getStatus, 1); // 只查询可预订的门票
        queryWrapper.orderByAsc(Ticket::getPrice); // 按价格升序
        
        return ticketMapper.selectPage(new Page<>(currentPage, size), queryWrapper);
    }
    
    /**
     * 更新门票库存
     */
    @Transactional
    public void updateTicketStock(Long ticketId, Integer quantity) {
        Ticket ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) {
            throw new ServiceException("门票不存在");
        }
        
        // 检查库存是否充足
        if (ticket.getStock() < quantity) {
            throw new ServiceException("门票库存不足");
        }
        
        // 更新库存
        ticket.setStock(ticket.getStock() - quantity);
        ticketMapper.updateById(ticket);
    }
    
    /**
     * 恢复门票库存（订单取消或退款时）
     */
    @Transactional
    public void restoreTicketStock(Long ticketId, Integer quantity) {
        Ticket ticket = ticketMapper.selectById(ticketId);
        if (ticket == null) {
            throw new ServiceException("门票不存在");
        }
        
        // 恢复库存
        ticket.setStock(ticket.getStock() + quantity);
        ticketMapper.updateById(ticket);
    }
} 