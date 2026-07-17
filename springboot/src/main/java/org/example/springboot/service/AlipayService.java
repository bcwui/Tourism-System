package org.example.springboot.service;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayTradePagePayModel;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePagePayRequest;
import jakarta.annotation.Resource;
import org.example.springboot.config.AlipayConfig;
import org.example.springboot.entity.TicketOrder;
import org.example.springboot.exception.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AlipayService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AlipayService.class);

    @Autowired(required = false)
    private AlipayConfig alipayConfig;

    @Resource
    private TicketOrderService ticketOrderService;

    private void requireAlipayConfig() {
        if (alipayConfig == null) {
            throw new ServiceException("支付宝未配置，当前服务不支持支付宝功能");
        }
    }

    public String createMockAlipayForm(Long orderId) {
        TicketOrder order = ticketOrderService.getOrderDetail(orderId);
        if (order == null) {
            throw new ServiceException("订单不存在");
        }
        LOGGER.info("生成模拟支付宝支付表单，订单ID: {}, 订单号: {}", orderId, order.getOrderNo());
        return "<div style='text-align: center; padding: 50px;'>正在加载支付页面...</div>";
    }

    public void mockAlipayPayment(Long orderId) {
        LOGGER.info("开始处理模拟支付宝支付，订单ID: {}", orderId);
        TicketOrder order = ticketOrderService.getOrderDetail(orderId);
        if (order == null) {
            throw new ServiceException("订单不存在");
        }
        if (order.getStatus() == 1) {
            LOGGER.info("订单已支付，无需重复处理");
            return;
        }
        if (order.getStatus() != 0) {
            throw new ServiceException("订单状态异常，无法支付");
        }
        try {
            Thread.sleep(1000);
            ticketOrderService.payOrder(orderId, "ALIPAY");
            LOGGER.info("模拟支付宝支付成功，订单ID: {}, 订单号: {}", orderId, order.getOrderNo());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServiceException("支付处理被中断");
        } catch (Exception e) {
            LOGGER.error("模拟支付宝支付失败，订单ID: {}, 错误: {}", orderId, e.getMessage(), e);
            throw new ServiceException("支付处理失败: " + e.getMessage());
        }
    }

    public String createAlipayForm(Long orderId) {
        requireAlipayConfig();
        TicketOrder order = ticketOrderService.getOrderDetail(orderId);
        if (order == null) {
            throw new ServiceException("订单不存在");
        }
        AlipayClient alipayClient = new DefaultAlipayClient(
                alipayConfig.getGateway(),
                alipayConfig.getAppId(),
                alipayConfig.getPrivateKey(),
                alipayConfig.getFormat(),
                alipayConfig.getCharset(),
                alipayConfig.getPublicKey(),
                alipayConfig.getSignType());
        AlipayTradePagePayRequest request = new AlipayTradePagePayRequest();
        request.setReturnUrl(alipayConfig.getReturnUrl());
        request.setNotifyUrl(alipayConfig.getNotifyUrl());
        AlipayTradePagePayModel model = new AlipayTradePagePayModel();
        model.setOutTradeNo(order.getOrderNo());
        model.setTotalAmount(order.getTotalAmount().toString());
        model.setSubject("门票预订-" + order.getTicketName());
        model.setProductCode("FAST_INSTANT_TRADE_PAY");
        request.setBizModel(model);
        try {
            return alipayClient.pageExecute(request).getBody();
        } catch (AlipayApiException e) {
            LOGGER.error("生成支付宝支付表单失败: {}", e.getMessage());
            throw new ServiceException("生成支付宝支付表单失败");
        }
    }

    public void handleAlipayReturn(Map<String, String> params) {
        requireAlipayConfig();
        try {
            LOGGER.info("开始处理支付宝同步回调，参数: {}", params);
            boolean signVerified = AlipaySignature.rsaCheckV1(
                    params,
                    alipayConfig.getPublicKey(),
                    alipayConfig.getCharset(),
                    alipayConfig.getSignType());
            if (!signVerified) {
                LOGGER.error("支付宝同步回调签名验证失败");
                throw new ServiceException("支付宝同步回调签名验证失败");
            }
            LOGGER.info("支付宝同步回调签名验证成功");
            String outTradeNo = params.get("out_trade_no");
            String tradeNo = params.get("trade_no");
            String tradeStatus = params.get("trade_status");
            LOGGER.info("支付宝同步回调 - 订单号: {}, 交易号: {}, 交易状态: {}", outTradeNo, tradeNo, tradeStatus);
            TicketOrder order = ticketOrderService.getOrderByOrderNo(outTradeNo);
            if (order == null) {
                LOGGER.error("订单不存在: {}", outTradeNo);
                throw new ServiceException("订单不存在");
            }
            LOGGER.info("找到订单: {}, 当前状态: {}", order.getId(), order.getStatus());
            if (order.getStatus() == 1) {
                LOGGER.info("订单已支付，无需重复处理");
                return;
            }
            if (tradeNo != null && !tradeNo.isEmpty()) {
                LOGGER.info("开始更新订单状态为已支付，订单ID: {}", order.getId());
                ticketOrderService.payOrder(order.getId(), "ALIPAY");
                LOGGER.info("订单支付状态更新成功，订单号: {}", outTradeNo);
            } else {
                LOGGER.error("交易号为空，无法确认支付状态");
                throw new ServiceException("交易号为空");
            }
        } catch (AlipayApiException e) {
            LOGGER.error("支付宝同步回调处理失败: {}", e.getMessage(), e);
            throw new ServiceException("支付宝同步回调处理失败: " + e.getMessage());
        } catch (Exception e) {
            LOGGER.error("处理支付宝同步回调时发生未知错误: {}", e.getMessage(), e);
            throw new ServiceException("处理支付宝同步回调失败: " + e.getMessage());
        }
    }

    public String handleAlipayNotify(Map<String, String> params) {
        requireAlipayConfig();
        try {
            boolean signVerified = AlipaySignature.rsaCheckV1(
                    params,
                    alipayConfig.getPublicKey(),
                    alipayConfig.getCharset(),
                    alipayConfig.getSignType());
            if (!signVerified) {
                LOGGER.error("支付宝异步通知签名验证失败");
                return "fail";
            }
            String outTradeNo = params.get("out_trade_no");
            String tradeNo = params.get("trade_no");
            String tradeStatus = params.get("trade_status");
            LOGGER.info("支付宝异步通知 - 订单号: {}, 交易号: {}, 交易状态: {}", outTradeNo, tradeNo, tradeStatus);
            if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)) {
                TicketOrder order = ticketOrderService.getOrderByOrderNo(outTradeNo);
                if (order == null) {
                    return "fail";
                }
                if (order.getStatus() == 1) {
                    return "success";
                }
                ticketOrderService.payOrder(order.getId(), "ALIPAY");
                return "success";
            }
            return "fail";
        } catch (Exception e) {
            LOGGER.error("处理支付宝异步通知失败: {}", e.getMessage());
            return "fail";
        }
    }
}
