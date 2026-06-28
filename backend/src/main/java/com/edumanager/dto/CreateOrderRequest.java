package com.edumanager.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 创建订单请求 DTO
 */
@Data
public class CreateOrderRequest {

    /** 学员ID */
    @NotNull(message = "学员ID不能为空")
    private Long studentId;

    /** 课时包ID */
    @NotNull(message = "课时包ID不能为空")
    private Long packageId;

    /** 支付用户的微信 OpenID（用于 JSAPI 支付） */
    @NotNull(message = "OpenID不能为空")
    private String openid;
}
