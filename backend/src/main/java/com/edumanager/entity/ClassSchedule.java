package com.edumanager.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 排课计划
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("class_schedule")
public class ClassSchedule {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属机构ID */
    private Long institutionId;

    /** 课程ID */
    private Long courseId;

    /** 上课教师ID */
    private Long teacherId;

    /** 教室名称 */
    private String classroom;

    /** 上课日期 */
    private LocalDate scheduleDate;

    /** 开始时间 */
    private LocalTime startTime;

    /** 结束时间 */
    private LocalTime endTime;

    /** 最大上课人数 */
    private Integer maxStudents;

    /**
     * 状态
     * <p>SCHEDULED - 已安排 / IN_PROGRESS - 进行中 / FINISHED - 已结束 / CANCELLED - 已取消</p>
     */
    private String status;

    /** 动态签到码 Token */
    private String qrCodeToken;

    /** 签到码过期时间 */
    private LocalDateTime qrCodeExpireAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
