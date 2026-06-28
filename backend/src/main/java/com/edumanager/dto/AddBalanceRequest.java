package com.edumanager.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.math.BigDecimal;

/**
 * 课时增加请求 DTO（购买课程后调用）
 */
@Data
public class AddBalanceRequest {

    /** 学员ID */
    @NotNull(message = "学员ID不能为空")
    private Long studentId;

    /** 课程ID */
    @NotNull(message = "课程ID不能为空")
    private Long courseId;

    /** 增加的课时数 */
    @NotNull(message = "课时数不能为空")
    @Positive(message = "课时数必须大于0")
    private BigDecimal amount;

    /** 关联订单ID */
    @NotNull(message = "订单ID不能为空")
    private Long orderId;

    /** 备注 */
    private String remark;
}
