package com.edumanager.service;

import com.edumanager.dto.RosterStudentVO;
import com.edumanager.dto.ScheduleRequest;
import com.edumanager.dto.ScheduleVO;

import java.time.LocalDate;
import java.util.List;

/**
 * 排课管理服务
 */
public interface ScheduleService {

    /** 创建排课（含初始点名册） */
    ScheduleVO createSchedule(Long institutionId, ScheduleRequest request);

    /** 更新排课信息 */
    ScheduleVO updateSchedule(Long scheduleId, ScheduleRequest request);

    /** 取消排课 */
    void cancelSchedule(Long scheduleId);

    /** 按日期范围查询排课列表 */
    List<ScheduleVO> querySchedules(Long institutionId, LocalDate startDate,
                                     LocalDate endDate, Long teacherId, Long courseId);

    /** 查询单节排课详情（含点名册） */
    ScheduleVO getScheduleDetail(Long scheduleId);

    /** 获取点名册学员列表 */
    List<RosterStudentVO> getRosterStudents(Long scheduleId);

    /** 向点名册添加学员 */
    void addStudentToRoster(Long scheduleId, Long studentId);

    /** 从点名册移除学员 */
    void removeStudentFromRoster(Long scheduleId, Long studentId);

    /** 刷新排课的动态二维码 Token */
    String refreshQrCode(Long scheduleId);
}
