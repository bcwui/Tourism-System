package org.example.springboot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.example.springboot.common.Result;
import org.example.springboot.entity.TicketOrder;
import org.example.springboot.entity.User;
import org.example.springboot.service.TicketOrderService;
import org.example.springboot.util.JwtTokenUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Tag(name="门票订单管理接口")
@RestController
@RequestMapping("/order")
public class TicketOrderController {
    private static final Logger LOGGER = LoggerFactory.getLogger(TicketOrderController.class);
    
    @Resource
    private TicketOrderService ticketOrderService;

    @Operation(summary = "创建门票订单")
    @PostMapping
    public Result<?> createOrder(@RequestBody TicketOrder order) {
        TicketOrder createdOrder = ticketOrderService.createOrder(order);
        return Result.success(createdOrder);
    }
    
    @Operation(summary = "支付订单")
    @PostMapping("/{id}/pay")
    public Result<?> payOrder(
            @PathVariable Long id, 
            @RequestParam String paymentMethod) {
        ticketOrderService.payOrder(id, paymentMethod);
        return Result.success();
    }
    
    @Operation(summary = "取消订单")
    @PostMapping("/{id}/cancel")
    public Result<?> cancelOrder(@PathVariable Long id) {
        ticketOrderService.cancelOrder(id);
        return Result.success();
    }
    
    @Operation(summary = "退款订单（管理员专用）")
    @PostMapping("/{id}/refund")
    public Result<?> refundOrder(@PathVariable Long id) {
        ticketOrderService.refundOrder(id);
        return Result.success();
    }
    
    @Operation(summary = "完成订单（管理员专用）")
    @PostMapping("/{id}/complete")
    public Result<?> completeOrder(@PathVariable Long id) {
        ticketOrderService.completeOrder(id);
        return Result.success();
    }
    
    @Operation(summary = "获取当前用户订单列表")
    @GetMapping("/my")
    public Result<?> getMyOrders(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer size) {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            return Result.error("用户未登录");
        }
        
        Page<TicketOrder> page = ticketOrderService.getUserOrders(currentUser.getId(), status, currentPage, size);
        return Result.success(page);
    }
    
    @Operation(summary = "获取订单详情")
    @GetMapping("/{id}")
    public Result<?> getOrderDetail(@PathVariable Long id) {
        TicketOrder order = ticketOrderService.getOrderDetail(id);
        return Result.success(order);
    }
    
    @Operation(summary = "根据订单号查询订单状态")
    @GetMapping("/status")
    public Result<?> getOrderStatus(@RequestParam String orderNo) {
        TicketOrder order = ticketOrderService.getOrderByOrderNo(orderNo);
        if (order == null) {
            return Result.error("订单不存在");
        }
        return Result.success(order);
    }
    
    @Operation(summary = "管理员查询所有订单")
    @GetMapping("/admin/page")
    public Result<?> getAllOrders(
            @RequestParam(defaultValue = "") String orderNo,
            @RequestParam(defaultValue = "") String visitorName,
            @RequestParam(defaultValue = "") String visitorPhone,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer size) {
        // 验证当前用户是否是管理员
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null || !"ADMIN".equals(currentUser.getRoleCode())) {
            return Result.error("无权访问");
        }
        
        Page<TicketOrder> page = ticketOrderService.getAllOrders(orderNo, visitorName, visitorPhone, status, currentPage, size);
        return Result.success(page);
    }
    
    @Operation(summary = "获取订单统计信息")
    @GetMapping("/stats")
    public Result<?> getOrderStats() {
        // 验证当前用户是否是管理员
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null || !"ADMIN".equals(currentUser.getRoleCode())) {
            return Result.error("无权访问");
        }
        
        // 这里可以实现订单统计逻辑，比如按状态统计数量、总金额等
        // 这里简单返回一个示例数据
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalOrders", 100);
        stats.put("totalAmount", 10000);
        stats.put("pendingPayment", 20);
        stats.put("paid", 70);
        stats.put("cancelled", 8);
        stats.put("refunded", 2);
        stats.put("completed", 0);
        
        return Result.success(stats);
    }
    
    @Operation(summary = "获取当前用户订单统计信息")
    @GetMapping("/my/stats")
    public Result<?> getMyOrderStats() {
        User currentUser = JwtTokenUtils.getCurrentUser();
        if (currentUser == null) {
            return Result.error("用户未登录");
        }
        
        Map<String, Object> stats = ticketOrderService.getUserOrderStats(currentUser.getId());
        return Result.success(stats);
    }

    @Operation(summary = "删除订单")
    @DeleteMapping("/{id}")
    public Result<?> deleteOrder(@PathVariable Long id) {
        ticketOrderService.deleteOrder(id);
        return Result.success("订单删除成功");
    }
}