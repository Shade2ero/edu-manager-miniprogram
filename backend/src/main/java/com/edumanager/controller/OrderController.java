package com.edumanager.controller;

import com.edumanager.common.Result;
import com.edumanager.dto.CreateOrderRequest;
import com.edumanager.dto.OrderResult;
import com.edumanager.service.CourseOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 订单 Controller — 创建订单、查询支付状态
 */
@Slf4j
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Validated
public class OrderController {

    private final CourseOrderService orderService;

    /**
     * 创建购买订单并获取微信支付参数
     *
     * <pre>
     * POST /api/orders
     * Body: {
     *   "studentId": 456,
     *   "packageId": 1,
     *   "openid": "oXXXXXXXXXXXX"
     * }
     * </pre>
     *
     * <p>返回 orderNo + 前端调起微信支付所需的 payParams</p>
     * <p>前端收到后立即调用 wx.requestPayment(payParams) 拉起支付</p>
     */
    @PostMapping
    public Result<OrderResult> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        log.info("创建订单: studentId={}, packageId={}", request.getStudentId(), request.getPackageId());
        OrderResult result = orderService.createOrder(request);
        return Result.ok(result);
    }

    /**
     * 查询订单支付状态（前端轮询兜底）
     *
     * <pre>
     * GET /api/orders/status?orderNo=EDU2024062712345678
     * </pre>
     *
     * <p>返回 PAID / PENDING / CLOSED / NOT_FOUND</p>
     */
    @GetMapping("/status")
    public Result<String> queryOrderStatus(@RequestParam String orderNo) {
        String status = orderService.syncOrderStatus(orderNo);
        return Result.ok(status);
    }

    /**
     * 【开发模式】模拟支付成功，跳过微信支付直接完成订单
     * <pre>POST /api/orders/mock-pay?orderNo=EDU2024062712345678</pre>
     */
    @PostMapping("/mock-pay")
    public Result<String> mockPay(@RequestParam String orderNo) {
        log.info("模拟支付: orderNo={}", orderNo);
        String notifyJson = "{\"out_trade_no\":\"" + orderNo
                + "\",\"transaction_id\":\"MOCK_" + System.currentTimeMillis()
                + "\",\"trade_state\":\"SUCCESS\"}";
        orderService.handlePaymentNotify(notifyJson);
        return Result.ok("模拟支付成功，课时已到账");
    }
}
