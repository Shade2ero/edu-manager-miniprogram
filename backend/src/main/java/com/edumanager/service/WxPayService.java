package com.edumanager.service;

import java.util.Map;

/**
 * 微信支付服务接口（V3 API）
 *
 * <p>封装微信支付 JSAPI 的核心能力：
 * 统一下单、签名生成、支付回调处理、订单查询</p>
 */
public interface WxPayService {

    /**
     * JSAPI 统一下单
     *
     * <p>调用微信支付 V3 /v3/pay/transactions/jsapi 接口，
     * 获取 prepay_id，并生成前端 wx.requestPayment 所需签名参数。</p>
     *
     * @param orderNo    商户订单号
     * @param openid     支付用户的微信 OpenID
     * @param amount     支付金额（分）
     * @param description 商品描述
     * @return 前端调起支付所需的参数 Map（含 timeStamp, nonceStr, package, signType, paySign）
     */
    Map<String, String> createJsapiOrder(String orderNo, String openid,
                                          int amount, String description);

    /**
     * 验证微信支付回调通知的签名
     *
     * @param body       回调请求体（JSON 原文）
     * @param signature  微信签名头 Wechatpay-Signature
     * @param timestamp  微信时间戳头 Wechatpay-Timestamp
     * @param nonce      微信随机数头 Wechatpay-Nonce
     * @return 回调通知的明文 JSON 字符串（验签通过后返回）
     */
    String verifyNotifySignature(String body, String signature,
                                  String timestamp, String nonce);

    /**
     * 查询订单支付状态
     *
     * @param orderNo 商户订单号
     * @return 订单状态：SUCCESS / NOTPAY / CLOSED / REFUND 等
     */
    String queryOrderStatus(String orderNo);
}
