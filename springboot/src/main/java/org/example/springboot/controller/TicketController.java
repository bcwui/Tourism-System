package org.example.springboot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.example.springboot.common.Result;
import org.example.springboot.entity.Ticket;
import org.example.springboot.service.TicketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@Tag(name="门票管理接口")
@RestController
@RequestMapping("/ticket")
public class TicketController {
    private static final Logger LOGGER = LoggerFactory.getLogger(TicketController.class);
    
    @Resource
    private TicketService ticketService;

    @Operation(summary = "分页查询门票")
    @GetMapping("/page")
    public Result<?> getTicketsByPage(
            @RequestParam(defaultValue = "") String ticketName,
            @RequestParam(defaultValue = "") String ticketType,
            @RequestParam(required = false) Long scenicId,
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer size) {
        Page<Ticket> page = ticketService.getTicketsByPage(ticketName, ticketType, scenicId, currentPage, size);
        return Result.success(page);
    }
    
    @Operation(summary = "根据ID获取门票详情")
    @GetMapping("/{id}")
    public Result<?> getTicketById(@PathVariable Long id) {
        Ticket ticket = ticketService.getTicketById(id);
        return Result.success(ticket);
    }
    
    @Operation(summary = "新增门票")
    @PostMapping
    public Result<?> addTicket(@RequestBody Ticket ticket) {
        ticketService.addTicket(ticket);
        return Result.success();
    }
    
    @Operation(summary = "更新门票信息")
    @PutMapping("/{id}")
    public Result<?> updateTicket(@PathVariable Long id, @RequestBody Ticket ticket) {
        ticket.setId(id);
        ticketService.updateTicket(ticket);
        return Result.success();
    }
    
    @Operation(summary = "删除门票")
    @DeleteMapping("/{id}")
    public Result<?> deleteTicket(@PathVariable Long id) {
        ticketService.deleteTicket(id);
        return Result.success();
    }
    
    @Operation(summary = "获取景点门票列表")
    @GetMapping("/scenic/{scenicId}")
    public Result<?> getTicketsByScenicId(
            @PathVariable Long scenicId,
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer size) {
        Page<Ticket> page = ticketService.getTicketsByScenicId(scenicId, currentPage, size);
        return Result.success(page);
    }
} 