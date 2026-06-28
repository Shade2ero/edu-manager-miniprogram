package com.edumanager.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.edumanager.service.WxPayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 微信支付 V3 API 服务实现
 *
 * <p><b>V3 API 核心流程：</b></p>
 * <ol>
 *   <li>服务端 POST /v3/pay/transactions/jsapi → 获取 prepay_id</li>
 *   <li>用商户私钥签名，生成 prepay_id 等参数返回前端</li>
 *   <li>前端 wx.requestPayment 调起支付</li>
 *   <li>微信回调通知 → 验签 → 更新订单状态 → 下发课时</li>
 * </ol>
 *
 * <p><b>配置项（application.yml）：</b></p>
 * <pre>
 * wx:
 *   pay:
 *     mch-id: 商户号
 *     mch-serial-no: 商户证书序列号
 *     api-v3-key: API v3 密钥
 *     private-key-path: /path/to/apiclient_key.pem
 *     app-id: 小程序AppId
 *     notify-url: https://your-domain.com/api/pay/notify
 * </pre>
 */
@Slf4j
@Service
public class WxPayServiceImpl implements WxPayService {

    @Value("${wx.pay.app-id}")
    private String appId;

    @Value("${wx.pay.mch-id}")
    private String mchId;

    @Value("${wx.pay.mch-serial-no}")
    private String mchSerialNo;

    @Value("${wx.pay.api-v3-key}")
    private String apiV3Key;

    @Value("${wx.pay.notify-url}")
    private String notifyUrl;

    /** 微信支付 V3 JSAPI 下单地址 */
    private static final String JSAPI_URL = "https://api.mch.weixin.qq.com/v3/pay/transactions/jsapi";

    @Override
    public Map<String, String> createJsapiOrder(String orderNo, String openid,
                                                 int amount, String description) {
        log.info("创建微信JSAPI订单: orderNo={}, openid={}, amount={}分", orderNo, openid, amount);

        // ===== 1. 构造请求体 =====
        JSONObject reqBody = new JSONObject();
        reqBody.set("appid", appId);
        reqBody.set("mchid", mchId);
        reqBody.set("description", description.length() > 127 ? description.substring(0, 127) : description);
        reqBody.set("out_trade_no", orderNo);
        reqBody.set("notify_url", notifyUrl);

        JSONObject amountObj = new JSONObject();
        amountObj.set("total", amount);
        amountObj.set("currency", "CNY");
        reqBody.set("amount", amountObj);

        JSONObject payerObj = new JSONObject();
        payerObj.set("openid", openid);
        reqBody.set("payer", payerObj);

        String reqBodyStr = reqBody.toString();

        // ===== 2. 生成 V3 签名（用于服务端调用微信接口） =====
        // TODO: 实际对接时，通过 OkHttp/Apache HttpClient 发起 HTTPS POST 请求
        // POST {JSAPI_URL}
        // Headers: Authorization: WECHATPAY2-SHA256-RSA2048 mchid="..",nonce_str="..",timestamp="..",serial_no="..",signature=".."
        // Body: reqBodyStr
        //
        // 此处为演示骨架，返回模拟 prepay_id
        // 生产环境需替换为真实的 HTTP 调用 + RSA 签名逻辑

        String prepayId = "prepay_" + IdUtil.fastSimpleUUID();
        log.info("获取prepay_id: {}（生产环境需实际调用微信API）", prepayId);

        // ===== 3. 生成前端 wx.requestPayment 所需的签名参数 =====
        return buildJsapiSignParams(prepayId);
    }

    @Override
    public String verifyNotifySignature(String body, String signature,
                                         String timestamp, String nonce) {
        // ===== 验签流程（V3 标准） =====
        // 1. 构造验签串：timestamp + "\n" + nonce + "\n" + body + "\n"
        // 2. 用微信支付平台公钥验签（或从微信获取平台证书）
        // 3. 验签通过后解析 body 中的 resource.ciphertext
        // 4. 用 APIv3 密钥 AES-256-GCM 解密 ciphertext 得到明文通知
        //
        // 完整实现见微信支付官方文档：
        // https://pay.weixin.qq.com/wiki/doc/apiv3/wechatpay/wechatpay4_1.shtml

        log.info("验证支付回调签名: timestamp={}, nonce={}", timestamp, nonce);

        // 生产环境：验签 + 解密
        // String plainText = aesGcmDecrypt(ciphertext, associatedData, nonce, apiV3Key);
        // return plainText;

        // 演示模式：直接返回 body 不做验签（不安全，仅供开发调试）
        log.warn("【开发模式】跳过支付回调验签，生产环境必须启用验签！");
        return body;
    }

    @Override
    public String queryOrderStatus(String orderNo) {
        // GET {JSAPI_URL}/{out_trade_no}?mchid={mchid}
        // 返回订单状态
        log.info("查询订单状态: orderNo={}", orderNo);
        return "NOTPAY"; // 演示
    }

    // ================================================================
    //  私有方法
    // ================================================================

    /**
     * 构建前端 wx.requestPayment 所需参数
     *
     * <p>前端调用示例：</p>
     * <pre>
     * wx.requestPayment({
     *   timeStamp: '',    // 时间戳（秒）
     *   nonceStr: '',     // 随机字符串
     *   package: 'prepay_id=wx...',  // prepay_id 包
     *   signType: 'RSA',  // V3 使用 RSA 签名
     *   paySign: '',      // 签名
     *   success: ...,
     *   fail: ...
     * })
     * </pre>
     */
    private Map<String, String> buildJsapiSignParams(String prepayId) {
        long timestamp = System.currentTimeMillis() / 1000;
        String nonceStr = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 32);

        // V3 签名串格式：appId + "\n" + timestamp + "\n" + nonceStr + "\n" + prepayId + "\n"
        String signStr = String.format("%s\n%s\n%s\nprepay_id=%s\n",
                appId, timestamp, nonceStr, prepayId);

        // TODO: 生产环境用商户私钥 RSA 签名
        // String paySign = rsaSign(signStr, privateKey);
        // 开发阶段使用 HMAC-SHA256 替代（仅用于联调，生产必须用 RSA）
        String paySign = hmacSha256Sign(signStr, apiV3Key);

        Map<String, String> params = new HashMap<>();
        params.put("timeStamp", String.valueOf(timestamp));
        params.put("nonceStr", nonceStr);
        params.put("package", "prepay_id=" + prepayId);
        params.put("signType", "RSA");
        params.put("paySign", paySign);
        return params;
    }

    /**
     * HMAC-SHA256 签名（开发环境临时方案，生产环境必须使用 RSA）
     */
    private String hmacSha256Sign(String data, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            log.error("HMAC签名失败", e);
            return "";
        }
    }
}
