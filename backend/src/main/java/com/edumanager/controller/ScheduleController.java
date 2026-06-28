package com.edumanager.controller;

import com.edumanager.common.Result;
import com.edumanager.dto.RosterStudentVO;
import com.edumanager.dto.ScheduleRequest;
import com.edumanager.dto.ScheduleVO;
import com.edumanager.service.ScheduleService;
import com.edumanager.util.QrCodeUtil;
import cn.hutool.json.JSONObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDate;
import java.util.List;

/**
 * 排课管理 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
@Validated
public class ScheduleController {

    private final ScheduleService scheduleService;

    /**
     * 创建排课
     * <pre>
     * POST /api/schedules?institutionId=1
     * Body: { courseId, teacherId, classroom, scheduleDate, startTime, endTime, maxStudents, studentIds }
     * </pre>
     */
    @PostMapping
    public Result<ScheduleVO> create(@RequestParam Long institutionId,
                                      @Valid @RequestBody ScheduleRequest request) {
        return Result.ok(scheduleService.createSchedule(institutionId, request));
    }

    /**
     * 查询排课列表（支持日期范围、教师、课程筛选）
     * <pre>
     * GET /api/schedules?institutionId=1&startDate=2026-06-01&endDate=2026-06-30&teacherId=2&courseId=1
     * </pre>
     */
    @GetMapping
    public Result<List<ScheduleVO>> list(
            @RequestParam Long institutionId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false) Long courseId) {
        if (startDate == null) startDate = LocalDate.now().withDayOfMonth(1);
        if (endDate == null) endDate = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth());
        return Result.ok(scheduleService.querySchedules(institutionId, startDate, endDate, teacherId, courseId));
    }

    /**
     * 查询排课详情（含点名册统计）
     * <pre>GET /api/schedules/123</pre>
     */
    @GetMapping("/{scheduleId}")
    public Result<ScheduleVO> detail(@PathVariable Long scheduleId) {
        return Result.ok(scheduleService.getScheduleDetail(scheduleId));
    }

    /**
     * 更新排课
     * <pre>PUT /api/schedules/123</pre>
     */
    @PutMapping("/{scheduleId}")
    public Result<ScheduleVO> update(@PathVariable Long scheduleId,
                                      @Valid @RequestBody ScheduleRequest request) {
        return Result.ok(scheduleService.updateSchedule(scheduleId, request));
    }

    /**
     * 取消排课
     * <pre>DELETE /api/schedules/123</pre>
     */
    @DeleteMapping("/{scheduleId}")
    public Result<Void> cancel(@PathVariable Long scheduleId) {
        scheduleService.cancelSchedule(scheduleId);
        return Result.ok();
    }

    /**
     * 获取点名册
     * <pre>GET /api/schedules/123/roster</pre>
     */
    @GetMapping("/{scheduleId}/roster")
    public Result<List<RosterStudentVO>> getRoster(@PathVariable Long scheduleId) {
        return Result.ok(scheduleService.getRosterStudents(scheduleId));
    }

    /**
     * 向点名册添加学员
     * <pre>POST /api/schedules/123/roster?studentId=456</pre>
     */
    @PostMapping("/{scheduleId}/roster")
    public Result<Void> addToRoster(@PathVariable Long scheduleId,
                                     @RequestParam Long studentId) {
        scheduleService.addStudentToRoster(scheduleId, studentId);
        return Result.ok();
    }

    /**
     * 从点名册移除学员
     * <pre>DELETE /api/schedules/123/roster?studentId=456</pre>
     */
    @DeleteMapping("/{scheduleId}/roster")
    public Result<Void> removeFromRoster(@PathVariable Long scheduleId,
                                          @RequestParam Long studentId) {
        scheduleService.removeStudentFromRoster(scheduleId, studentId);
        return Result.ok();
    }

    /**
     * 刷新二维码 Token
     * <pre>POST /api/schedules/123/qrcode/refresh</pre>
     */
    @PostMapping("/{scheduleId}/qrcode/refresh")
    public Result<String> refreshQrCode(@PathVariable Long scheduleId) {
        return Result.ok(scheduleService.refreshQrCode(scheduleId));
    }

    /**
     * 获取排课二维码图片（Base64 PNG，教师端展示给学生扫码用）
     * <pre>GET /api/schedules/123/qrcode</pre>
     *
     * <p>返回二维码图片的 Base64 data URI，可直接放入 image 标签 src</p>
     */
    @GetMapping("/{scheduleId}/qrcode")
    public Result<String> getQrCodeImage(@PathVariable Long scheduleId) {
        ScheduleVO vo = scheduleService.getScheduleDetail(scheduleId);
        if (vo.getQrCodeToken() == null || vo.getQrCodeToken().isEmpty()) {
            return Result.fail("该排课尚未生成签到二维码");
        }
        // 二维码内容：JSON 格式，与学生端扫码解析逻辑对应
        JSONObject qrContent = new JSONObject();
        qrContent.set("scheduleId", scheduleId);
        qrContent.set("token", vo.getQrCodeToken());
        String base64 = QrCodeUtil.generateBase64(qrContent.toString(), 400);
        return Result.ok(base64);
    }
}
