package com.edumanager.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 创建/更新排课请求 DTO
 */
@Data
public class ScheduleRequest {

    /** 课程ID */
    @NotNull(message = "课程ID不能为空")
    private Long courseId;

    /** 教师ID */
    @NotNull(message = "教师ID不能为空")
    private Long teacherId;

    /** 教室名称 */
    private String classroom;

    /** 上课日期 */
    @NotNull(message = "上课日期不能为空")
    private LocalDate scheduleDate;

    /** 开始时间 */
    @NotNull(message = "开始时间不能为空")
    private LocalTime startTime;

    /** 结束时间 */
    @NotNull(message = "结束时间不能为空")
    private LocalTime endTime;

    /** 最大上课人数 */
    private Integer maxStudents = 20;

    /** 学员ID列表（点名册初始数据） */
    private List<Long> studentIds;
}
