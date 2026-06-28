package com.edumanager.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edumanager.dto.RosterStudentVO;
import com.edumanager.dto.ScheduleRequest;
import com.edumanager.dto.ScheduleVO;
import com.edumanager.entity.*;
import com.edumanager.exception.BusinessException;
import com.edumanager.mapper.*;
import com.edumanager.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleServiceImpl implements ScheduleService {

    private final ClassScheduleMapper scheduleMapper;
    private final ClassStudentMapper classStudentMapper;
    private final StudentBalanceMapper balanceMapper;
    private final CourseMapper courseMapper;
    private final StudentMapper studentMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ScheduleVO createSchedule(Long institutionId, ScheduleRequest request) {
        // 1. 生成二维码 Token：UUID（每次重新生成，防盗用）
        String qrToken = IdUtil.fastSimpleUUID();

        ClassSchedule schedule = ClassSchedule.builder()
                .institutionId(institutionId)
                .courseId(request.getCourseId())
                .teacherId(request.getTeacherId())
                .classroom(request.getClassroom())
                .scheduleDate(request.getScheduleDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .maxStudents(request.getMaxStudents())
                .status("SCHEDULED")
                .qrCodeToken(qrToken)
                .qrCodeExpireAt(LocalDateTime.now().plusHours(24))
                .build();
        scheduleMapper.insert(schedule);

        // 2. 初始化点名册（如果有传入学员列表）
        if (request.getStudentIds() != null && !request.getStudentIds().isEmpty()) {
            List<ClassStudent> roster = new ArrayList<>();
            for (Long studentId : request.getStudentIds()) {
                roster.add(ClassStudent.builder()
                        .scheduleId(schedule.getId())
                        .studentId(studentId)
                        .status("PENDING")
                        .build());
            }
            // 批量插入
            for (ClassStudent cs : roster) {
                classStudentMapper.insert(cs);
            }
        }

        log.info("创建排课成功: scheduleId={}, courseId={}, date={}, token={}",
                schedule.getId(), request.getCourseId(), request.getScheduleDate(), qrToken);
        return buildScheduleVO(schedule);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ScheduleVO updateSchedule(Long scheduleId, ScheduleRequest request) {
        ClassSchedule schedule = scheduleMapper.selectById(scheduleId);
        if (schedule == null) throw new BusinessException(30001, "排课不存在");

        schedule.setCourseId(request.getCourseId());
        schedule.setTeacherId(request.getTeacherId());
        schedule.setClassroom(request.getClassroom());
        schedule.setScheduleDate(request.getScheduleDate());
        schedule.setStartTime(request.getStartTime());
        schedule.setEndTime(request.getEndTime());
        schedule.setMaxStudents(request.getMaxStudents());
        scheduleMapper.updateById(schedule);
        log.info("更新排课: scheduleId={}", scheduleId);
        return buildScheduleVO(schedule);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelSchedule(Long scheduleId) {
        ClassSchedule schedule = scheduleMapper.selectById(scheduleId);
        if (schedule == null) throw new BusinessException(30001, "排课不存在");
        schedule.setStatus("CANCELLED");
        scheduleMapper.updateById(schedule);
        log.info("取消排课: scheduleId={}", scheduleId);
    }

    @Override
    public List<ScheduleVO> querySchedules(Long institutionId, LocalDate startDate,
                                            LocalDate endDate, Long teacherId, Long courseId) {
        LambdaQueryWrapper<ClassSchedule> wrapper = new LambdaQueryWrapper<ClassSchedule>()
                .eq(ClassSchedule::getInstitutionId, institutionId)
                .ge(startDate != null, ClassSchedule::getScheduleDate, startDate)
                .le(endDate != null, ClassSchedule::getScheduleDate, endDate)
                .eq(teacherId != null, ClassSchedule::getTeacherId, teacherId)
                .eq(courseId != null, ClassSchedule::getCourseId, courseId)
                .orderByAsc(ClassSchedule::getScheduleDate)
                .orderByAsc(ClassSchedule::getStartTime);

        List<ClassSchedule> schedules = scheduleMapper.selectList(wrapper);
        return schedules.stream().map(this::buildScheduleVO).collect(Collectors.toList());
    }

    @Override
    public ScheduleVO getScheduleDetail(Long scheduleId) {
        ClassSchedule schedule = scheduleMapper.selectById(scheduleId);
        if (schedule == null) throw new BusinessException(30001, "排课不存在");
        return buildScheduleVO(schedule);
    }

    @Override
    public List<RosterStudentVO> getRosterStudents(Long scheduleId) {
        List<ClassStudent> roster = classStudentMapper.selectList(
                new LambdaQueryWrapper<ClassStudent>()
                        .eq(ClassStudent::getScheduleId, scheduleId)
        );

        if (roster.isEmpty()) return Collections.emptyList();

        // 批量查学员真实姓名
        final Map<Long, Student> studentMap;
        List<Long> ids = roster.stream()
                .map(ClassStudent::getStudentId).collect(Collectors.toList());
        if (!ids.isEmpty()) {
            studentMap = studentMapper.selectBatchIds(ids).stream()
                    .collect(Collectors.toMap(Student::getId, s -> s));
        } else {
            studentMap = new HashMap<>();
        }

        // 查询排课对应的课程ID
        ClassSchedule schedule = scheduleMapper.selectById(scheduleId);
        Long courseId = schedule != null ? schedule.getCourseId() : null;

        // 批量查余额
        Map<Long, java.math.BigDecimal> balanceMap = new HashMap<>();
        if (courseId != null) {
            List<Long> studentIds = roster.stream()
                    .map(ClassStudent::getStudentId).collect(Collectors.toList());
            List<StudentBalance> balances = balanceMapper.selectList(
                    new LambdaQueryWrapper<StudentBalance>()
                            .in(StudentBalance::getStudentId, studentIds)
                            .eq(StudentBalance::getCourseId, courseId)
            );
            for (StudentBalance b : balances) {
                balanceMap.put(b.getStudentId(), b.getRemaining());
            }
        }

        return roster.stream().map(cs -> RosterStudentVO.builder()
                .classStudentId(cs.getId())
                .studentId(cs.getStudentId())
                .studentName(studentMap.containsKey(cs.getStudentId())
                        ? studentMap.get(cs.getStudentId()).getRealName()
                        : "学员" + cs.getStudentId())
                .status(cs.getStatus())
                .checkInTime(cs.getCheckInTime())
                .checkInMethod(cs.getCheckInMethod())
                .remainingHours(balanceMap.getOrDefault(cs.getStudentId(), java.math.BigDecimal.ZERO))
                .riskFlag(cs.getRiskFlag())
                .riskReason(cs.getRiskReason())
                .build()).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addStudentToRoster(Long scheduleId, Long studentId) {
        // 防重复
        Long count = classStudentMapper.selectCount(
                new LambdaQueryWrapper<ClassStudent>()
                        .eq(ClassStudent::getScheduleId, scheduleId)
                        .eq(ClassStudent::getStudentId, studentId)
        );
        if (count > 0) throw new BusinessException(30002, "该学员已在点名册中");

        classStudentMapper.insert(ClassStudent.builder()
                .scheduleId(scheduleId).studentId(studentId).status("PENDING").build());
        log.info("添加学员到点名册: scheduleId={}, studentId={}", scheduleId, studentId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeStudentFromRoster(Long scheduleId, Long studentId) {
        classStudentMapper.delete(
                new LambdaQueryWrapper<ClassStudent>()
                        .eq(ClassStudent::getScheduleId, scheduleId)
                        .eq(ClassStudent::getStudentId, studentId)
        );
        log.info("从点名册移除学员: scheduleId={}, studentId={}", scheduleId, studentId);
    }

    @Override
    public String refreshQrCode(Long scheduleId) {
        ClassSchedule schedule = scheduleMapper.selectById(scheduleId);
        if (schedule == null) throw new BusinessException(30001, "排课不存在");

        String newToken = IdUtil.fastSimpleUUID();
        schedule.setQrCodeToken(newToken);
        // 过期时间：取「课程结束后30分钟」和「当前时间+24小时」中较晚的
        LocalDateTime classEndExpiry = LocalDateTime.of(schedule.getScheduleDate(), schedule.getEndTime()).plusMinutes(30);
        LocalDateTime nowExpiry = LocalDateTime.now().plusHours(24);
        schedule.setQrCodeExpireAt(classEndExpiry.isAfter(nowExpiry) ? classEndExpiry : nowExpiry);
        scheduleMapper.updateById(schedule);
        log.info("刷新二维码Token: scheduleId={}, newToken={}", scheduleId, newToken);
        return newToken;
    }

    // ==================== VO 组装 ====================

    private ScheduleVO buildScheduleVO(ClassSchedule s) {
        // 课程名称
        Course course = courseMapper.selectById(s.getCourseId());
        String courseName = course != null ? course.getName() : "未知";

        // 教师名称（简化）
        String teacherName = "教师" + s.getTeacherId();

        // 点名册统计
        List<ClassStudent> roster = classStudentMapper.selectList(
                new LambdaQueryWrapper<ClassStudent>()
                        .eq(ClassStudent::getScheduleId, s.getId())
        );
        int total = roster.size();
        int checkedIn = (int) roster.stream().filter(r -> "CHECKED_IN".equals(r.getStatus())).count();
        int absent = (int) roster.stream().filter(r -> "ABSENT".equals(r.getStatus())).count();
        int leave = (int) roster.stream().filter(r -> "LEAVE".equals(r.getStatus())).count();
        int pending = total - checkedIn - absent - leave;

        return ScheduleVO.builder()
                .scheduleId(s.getId())
                .courseId(s.getCourseId())
                .courseName(courseName)
                .teacherId(s.getTeacherId())
                .teacherName(teacherName)
                .classroom(s.getClassroom())
                .scheduleDate(s.getScheduleDate())
                .startTime(s.getStartTime())
                .endTime(s.getEndTime())
                .maxStudents(s.getMaxStudents())
                .status(s.getStatus())
                .qrCodeToken(s.getQrCodeToken())
                .qrCodeExpireAt(s.getQrCodeExpireAt() != null ? s.getQrCodeExpireAt().toString() : null)
                .totalStudents(total)
                .checkedInCount(checkedIn)
                .absentCount(absent)
                .leaveCount(leave)
                .pendingCount(pending)
                .consumedCount(checkedIn - leave) // 简化估算
                .insufficientCount(0)
                .build();
    }
}
