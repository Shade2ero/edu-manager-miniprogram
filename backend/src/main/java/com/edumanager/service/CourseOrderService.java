package com.edumanager.service;

import com.edumanager.dto.CreateOrderRequest;
import com.edumanager.dto.OrderResult;

/**
 * 课程购买订单服务
 *
 * <p><b>完整购买流程：</b></p>
 * <ol>
 *   <li>前端选择课时包 → POST /api/orders 创建订单（状态 PENDING）</li>
 *   <li>后端返回微信支付参数 → 前端 wx.requestPayment 拉起支付</li>
 *   <li>支付成功 → 微信回调 notify_url → 后端更新订单为 PAID</li>
 *   <li>订单状态变为 PAID → 自动调用 StudentBalanceService.addBalance 下发课时</li>
 * </ol>
 */
public interface CourseOrderService {

    /**
     * 创建订单并返回微信支付参数
     *
     * @param request 下单请求（学员ID、课时包ID、OpenID）
     * @return 订单信息 + 微信支付参数
     */
    OrderResult createOrder(CreateOrderRequest request);

    /**
     * 处理微信支付回调通知
     *
     * <p>支付成功后：</p>
     * <ol>
     *   <li>更新 course_order 状态为 PAID</li>
     *   <li>调用 StudentBalanceService.addBalance 下发病时</li>
     *   <li>记录课时流水</li>
     * </ol>
     *
     * @param notifyJson 微信回调的明文 JSON
     */
    void handlePaymentNotify(String notifyJson);

    /**
     * 同步查询订单支付状态（兜底方案）
     *
     * <p>用于前端轮询或支付回调丢失时的主动查询</p>
     *
     * @param orderNo 订单号
     * @return 订单状态
     */
    String syncOrderStatus(String orderNo);
}
