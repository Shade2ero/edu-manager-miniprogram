package com.edumanager.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 排课-学员关联表（点名册）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("class_student")
public class ClassStudent {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 排课ID */
    private Long scheduleId;

    /** 学员ID */
    private Long studentId;

    /**
     * 签到状态
     * <p>PENDING - 未签到 / CHECKED_IN - 已签到 / ABSENT - 缺勤 / LEAVE - 请假</p>
     */
    private String status;

    /** 签到时间 */
    private LocalDateTime checkInTime;

    /** 签到方式：QR_SCAN / LBS / TEACHER_MANUAL */
    private String checkInMethod;

    /** 签到位置纬度 */
    private BigDecimal checkInLat;

    /** 签到位置经度 */
    private BigDecimal checkInLng;

    /** 签到设备信息 */
    private String checkInDevice;

    /** 风控标记：0-正常 1-疑似异常 */
    private Integer riskFlag;

    /** 风控原因 */
    private String riskReason;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
