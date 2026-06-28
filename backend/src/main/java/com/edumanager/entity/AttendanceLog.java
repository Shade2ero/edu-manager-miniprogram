package com.edumanager.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 签到日志 — 审计与追溯
 * <p>记录每次签到的详细信息，包含位置、设备、风控结果和课消状态</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("attendance_log")
public class AttendanceLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属机构ID */
    private Long institutionId;

    /** 排课ID */
    private Long scheduleId;

    /** 关联 class_student.id */
    private Long classStudentId;

    /** 学员ID */
    private Long studentId;

    /** 签到方式：QR_SCAN / LBS / TEACHER_MANUAL */
    private String checkInMethod;

    /** 签到时间 */
    private LocalDateTime checkInTime;

    /** 签到纬度 */
    private BigDecimal latitude;

    /** 签到经度 */
    private BigDecimal longitude;

    /** 距签到中心点距离（米） */
    private BigDecimal distanceFromCenter;

    /** 设备信息 JSON */
    private String deviceInfo;

    /** 微信原生定位验证是否通过 */
    private Integer wxLocationVerify;

    /** 签到IP */
    private String ipAddress;

    /** 风控评分 0-100 */
    private Integer riskScore;

    /** 风控明细 JSON */
    private String riskDetail;

    /**
     * 课消结果
     * <p>0-未触发 1-扣减成功 2-余额不足 3-跳过（请假/缺勤）</p>
     */
    private Integer consumeResult;

    /** 关联 balance_transaction.id */
    private Long consumeTransactionId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
