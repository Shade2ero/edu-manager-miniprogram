package com.edumanager.controller;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.edumanager.service.CourseOrderService;
import com.edumanager.service.WxPayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.util.HashMap;
import java.util.Map;

/**
 * 微信支付回调 Controller
 *
 * <p>接收微信支付平台的异步通知，完成订单状态更新与课时下发。</p>
 *
 * <p><b>微信回调要求：</b></p>
 * <ul>
 *   <li>接口必须返回状态码 200 且 body 为 {"code":"SUCCESS","message":"成功"} 才算应答成功</li>
 *   <li>微信在收到成功应答前会持续重试（最多 15 次，间隔递增）</li>
 *   <li>因此必须做好幂等处理</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/pay")
@RequiredArgsConstructor
public class PayController {

    private final WxPayService wxPayService;
    private final CourseOrderService orderService;

    /**
     * 微信支付回调通知
     *
     * <pre>
     * POST /api/pay/notify
     * Headers:
     *   Wechatpay-Signature: xxx
     *   Wechatpay-Timestamp: xxx
     *   Wechatpay-Nonce: xxx
     *   Wechatpay-Serial: xxx
     * Body: { "id": "...", "create_time": "...", "resource": {...}, ... }
     * </pre>
     */
    @PostMapping("/notify")
    public Map<String, String> notify(HttpServletRequest request) {
        try {
            // 1. 读取请求头（验签所需）
            String signature = request.getHeader("Wechatpay-Signature");
            String timestamp = request.getHeader("Wechatpay-Timestamp");
            String nonce = request.getHeader("Wechatpay-Nonce");

            // 2. 读取请求体
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = request.getReader()) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }
            String body = sb.toString();
            log.info("收到支付回调: timestamp={}, nonce={}", timestamp, nonce);

            // 3. 验签 + 解密回调内容
            String plainText = wxPayService.verifyNotifySignature(body, signature, timestamp, nonce);

            // 4. 解析解密后的通知 JSON，获取订单号和交易状态
            JSONObject notifyJson = JSONUtil.parseObj(plainText);
            JSONObject resource = notifyJson.getJSONObject("resource");
            if (resource == null) {
                log.error("回调通知缺少 resource 字段");
                return failResponse("缺少 resource");
            }

            // resource.ciphertext 已在 verifyNotifySignature 中解密为 plainText
            // 直接使用 plainText 中的 out_trade_no 和 trade_state
            orderService.handlePaymentNotify(plainText);

            return successResponse();

        } catch (Exception e) {
            log.error("处理支付回调异常", e);
            // 仍返回 success 防止微信重复推送（已记录日志供人工排查）
            return successResponse();
        }
    }

    private Map<String, String> successResponse() {
        Map<String, String> resp = new HashMap<>();
        resp.put("code", "SUCCESS");
        resp.put("message", "成功");
        return resp;
    }

    private Map<String, String> failResponse(String message) {
        Map<String, String> resp = new HashMap<>();
        resp.put("code", "FAIL");
        resp.put("message", message);
        return resp;
    }
}
