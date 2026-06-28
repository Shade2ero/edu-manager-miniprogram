package com.edumanager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 下单返回结果 — 含订单信息 + 微信支付参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResult {

    /** 订单号 */
    private String orderNo;

    /** 订单金额（分） */
    private Integer totalAmount;

    /** 课程名称 */
    private String courseName;

    /** 课时包名称 */
    private String packageName;

    /** 购买课时数 */
    private Integer totalHours;

    /** 前端调起微信支付所需的参数 */
    private Map<String, String> payParams;
}
