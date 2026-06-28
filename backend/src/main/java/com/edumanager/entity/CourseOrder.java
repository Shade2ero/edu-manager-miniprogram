package com.edumanager.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 购买订单
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("course_order")
public class CourseOrder {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 订单号（雪花ID） */
    private String orderNo;

    /** 所属机构ID */
    private Long institutionId;

    /** 学员ID */
    private Long studentId;

    /** 购买的课时包ID */
    private Long packageId;

    /** 冗余-关联课程ID */
    private Long courseId;

    /** 订单金额（分） */
    private Integer totalAmount;

    /** 实付金额（分） */
    private Integer paidAmount;

    /**
     * 订单状态
     * <p>PENDING - 待支付 / PAID - 已支付 / REFUNDING - 退款中 / REFUNDED - 已退款 / CLOSED - 已关闭</p>
     */
    private String status;

    /** 支付完成时间 */
    private LocalDateTime payTime;

    /** 微信支付流水号 */
    private String wxTransactionId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
