package com.edumanager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 排课展示 VO（含点名册统计）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleVO {

    private Long scheduleId;
    private Long courseId;
    private String courseName;
    private Long teacherId;
    private String teacherName;
    private String classroom;
    private LocalDate scheduleDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer maxStudents;
    private String status;          // SCHEDULED/IN_PROGRESS/FINISHED/CANCELLED
    private String qrCodeToken;     // 动态签到码 Token
    private String qrCodeExpireAt;  // 签到码过期时间

    // 点名册统计
    private int totalStudents;      // 总人数
    private int checkedInCount;     // 已签到
    private int absentCount;        // 缺勤
    private int leaveCount;         // 请假
    private int pendingCount;       // 未签到

    // 课消统计
    private int consumedCount;      // 已扣课
    private int insufficientCount;  // 余额不足未扣
}
