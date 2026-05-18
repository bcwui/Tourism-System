package org.example.springboot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.springboot.common.Result;
import org.example.springboot.entity.TicketOrder;
import org.example.springboot.service.AlipayService;
import org.example.springboot.service.TicketOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Tag(name="支付宝支付接口")
@RestController
@RequestMapping("/alipay")
public class AlipayController {
    private static final Logger LOGGER = LoggerFactory.getLogger(AlipayController.class);
    
    @Resource
    private AlipayService alipayService;
    
    @Resource
    private TicketOrderService ticketOrderService;

    @Operation(summary = "生成支付宝支付表单")
    @GetMapping("/pay/{orderId}")
    public Result<String> pay(@PathVariable Long orderId) {
        String formHtml = alipayService.createAlipayForm(orderId);
        return Result.success(formHtml);
    }
    
    @Operation(summary = "模拟支付宝支付")
    @PostMapping("/mock-pay/{orderId}")
    public Result<?> mockPay(@PathVariable Long orderId) {
        try {
            alipayService.mockAlipayPayment(orderId);
            return Result.success("支付成功");
        } catch (Exception e) {
            LOGGER.error("模拟支付失败: {}", e.getMessage(), e);
            return Result.error("支付失败: " + e.getMessage());
        }
    }
    
    @Operation(summary = "支付宝同步回调接口")
    @GetMapping("/return")
    public void returnUrl(HttpServletRequest request, HttpServletResponse response) throws IOException {
        LOGGER.info("收到支付宝同步回调请求");
        Map<String, String> params = convertRequestParametersToMap(request);
        
        // 打印所有参数用于调试
        LOGGER.info("支付宝回调参数: {}", params);
        
        String outTradeNo = params.get("out_trade_no");
        String redirectUrl;
        
        try {
            alipayService.handleAlipayReturn(params);
            // 支付处理成功，重定向到成功页面
            redirectUrl = "http://localhost:8080/payment/result?out_trade_no=" + outTradeNo + "&status=success";
            LOGGER.info("支付成功，重定向到: {}", redirectUrl);
        } catch (Exception e) {
            LOGGER.error("处理支付宝同步回调失败: {}", e.getMessage(), e);

            // 即使处理失败，也要检查订单状态
            // 因为可能是sendOrderMessage失败，但订单状态已经更新成功
            try {
                TicketOrder order = ticketOrderService.getOrderByOrderNo(outTradeNo);
                if (order != null && order.getStatus() == 1) {
                    // 订单状态已经是已支付，说明支付成功，只是消息发送失败
                    redirectUrl = "http://localhost:8080/payment/result?out_trade_no=" + outTradeNo + "&status=success";
                    LOGGER.info("订单状态已更新为已支付，重定向到成功页面: {}", redirectUrl);
                } else {
                    // 订单状态未更新，确实是支付失败
                    redirectUrl = "http://localhost:8080/payment/result?out_trade_no=" + outTradeNo + "&status=failed";
                    LOGGER.info("订单状态未更新，重定向到失败页面: {}", redirectUrl);
                }
            } catch (Exception ex) {
                // 查询订单也失败，重定向到失败页面
                redirectUrl = "http://localhost:8080/payment/result?out_trade_no=" + outTradeNo + "&status=failed";
                LOGGER.error("查询订单状态失败，重定向到失败页面: {}", redirectUrl, ex);
            }
        }
        
        response.sendRedirect(redirectUrl);
    }
    
    @Operation(summary = "支付宝异步通知接口")
    @PostMapping("/notify")
    public String notifyUrl(HttpServletRequest request) {
        LOGGER.info("收到支付宝异步通知请求");
        Map<String, String> params = convertRequestParametersToMap(request);
        
        return alipayService.handleAlipayNotify(params);
    }
    
    /**
     * 将请求参数转换为Map
     */
    private Map<String, String> convertRequestParametersToMap(HttpServletRequest request) {
        Map<String, String[]> parameterMap = request.getParameterMap();
        Map<String, String> params = new HashMap<>();
        
        for (String key : parameterMap.keySet()) {
            String[] values = parameterMap.get(key);
            String valueStr = "";
            for (int i = 0; i < values.length; i++) {
                valueStr = (i == values.length - 1) ? valueStr + values[i] : valueStr + values[i] + ",";
            }
            params.put(key, valueStr);
        }
        
        return params;
    }
}