package com.edumanager.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edumanager.dto.CheckInRequest;
import com.edumanager.dto.CheckInResult;
import com.edumanager.dto.ConsumeRequest;
import com.edumanager.dto.ConsumeResult;
import com.edumanager.entity.*;
import com.edumanager.exception.BusinessException;
import com.edumanager.mapper.*;
import com.edumanager.service.CheckInService;
import com.edumanager.service.StudentBalanceService;
import com.edumanager.util.DeviceFingerprintUtil;
import com.edumanager.util.GeoDistanceUtil;
import com.edumanager.util.RiskAssessmentEngine;
import com.edumanager.util.RiskAssessmentEngine.RiskAssessmentContext;
import com.edumanager.util.RiskAssessmentEngine.RiskAssessmentResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 签到服务实现
 *
 * <p><b>完整的签到 → 防作弊 → 课消处理链路</b></p>
 *
 * <p><b>异常边界处理：</b></p>
 * <ul>
 *   <li>Token 过期 → 返回明确错误提示</li>
 *   <li>重复签到 → 返回"已签到"，不重复扣课</li>
 *   <li>不在上课名单 → 拒绝签到</li>
 *   <li>LBS 严重越界 → 高危标记，需教师确认</li>
 *   <li>课消失败（余额不足）→ 签到仍然有效，只是不扣课</li>
 * </ul>
 *
 * @see StudentBalanceServiceImpl 课消服务
 * @see RiskAssessmentEngine 风控评估器
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CheckInServiceImpl implements CheckInService {

    private final ClassScheduleMapper scheduleMapper;
    private final ClassStudentMapper classStudentMapper;
    private final AttendanceLogMapper attendanceLogMapper;
    private final InstitutionMapper institutionMapper;
    private final StudentBalanceMapper balanceMapper;
    private final StudentBalanceService studentBalanceService;

    // ================================================================
    //  学生扫码签到（核心接口）
    // ================================================================

    /**
     * 处理学生签到请求。
     *
     * <p><b>整体事务策略：</b></p>
     * <p>签到写入 + 课消在同一个事务内完成。
     * 课消失败（如余额不足）不会回滚签到记录——签到是事实，课消是附加操作。
     * 因此 consumeHours 内部自己消化了失败状态，
     * 这里根据返回的 ConsumeResult.success 更新 attendance_log 的 consume_result 字段。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CheckInResult checkIn(CheckInRequest request) {
        log.info("==================== 签到流程开始 ====================");
        log.info("签到参数: scheduleId={}, studentId={}, lat={}, lng={}, device={} {}",
                request.getScheduleId(), request.getStudentId(),
                request.getLatitude(), request.getLongitude(),
                request.getDeviceBrand(), request.getDeviceModel());

        LocalDateTime now = LocalDateTime.now();

        // ================================================================
        //  阶段 A：身份与凭证核验
        // ================================================================

        // A1. 查询排课信息，校验排课存在性
        ClassSchedule schedule = scheduleMapper.selectById(request.getScheduleId());
        if (schedule == null) {
            log.warn("排课不存在: scheduleId={}", request.getScheduleId());
            return CheckInResult.fail("该课程安排不存在或已取消");
        }

        // A2. 校验二维码 Token（扫码签到时必传）
        if (request.getQrToken() != null && !request.getQrToken().isEmpty()) {
            String validateResult = validateQrToken(schedule, request.getQrToken(), now);
            if (validateResult != null) {
                return CheckInResult.fail(validateResult);
            }
        }
        // 注意：LBS 签到时 qrToken 可能为空，此时跳过 token 校验

        // A3. 查询点名册，不存在则查课时余额 → 已购课则自动加入
        ClassStudent classStudent = classStudentMapper.selectOne(
                new LambdaQueryWrapper<ClassStudent>()
                        .eq(ClassStudent::getScheduleId, request.getScheduleId())
                        .eq(ClassStudent::getStudentId, request.getStudentId())
        );
        if (classStudent == null) {
            // 检查是否购买了该课程
            StudentBalance balance = balanceMapper.selectOne(
                    new LambdaQueryWrapper<StudentBalance>()
                            .eq(StudentBalance::getStudentId, request.getStudentId())
                            .eq(StudentBalance::getCourseId, schedule.getCourseId())
                            .gt(StudentBalance::getRemaining, 0)
            );
            if (balance != null) {
                // 已购课 → 自动加入点名册
                classStudent = ClassStudent.builder()
                        .scheduleId(schedule.getId())
                        .studentId(request.getStudentId())
                        .status("PENDING")
                        .build();
                classStudentMapper.insert(classStudent);
                log.info("已购课学员自动加入点名册: scheduleId={}, studentId={}",
                        schedule.getId(), request.getStudentId());
            } else {
                log.warn("学员未购课且不在名单中: scheduleId={}, studentId={}",
                        request.getScheduleId(), request.getStudentId());
                return CheckInResult.fail("您未购买该课程，无法签到");
            }
        }

        // A4. 防重复签到
        if (!"PENDING".equals(classStudent.getStatus())) {
            log.info("学员已签到/请假，无需重复签到: status={}", classStudent.getStatus());
            return CheckInResult.fail(
                    "LEAVE".equals(classStudent.getStatus())
                            ? "您已请假，无需签到"
                            : "您已签到成功，请勿重复签到");
        }

        // ================================================================
        //  阶段 B：防作弊校验（六维度综合风控）
        // ================================================================

        // B1. 查询机构信息（获取 LBS 围栏中心点和半径）
        Institution institution = institutionMapper.selectById(schedule.getInstitutionId());
        if (institution == null || institution.getStatus() == 0) {
            return CheckInResult.fail("机构信息异常，请联系管理员");
        }

        // B2. 计算签到位置距机构中心点的距离（Haversine 公式）
        BigDecimal distanceFromCenter = GeoDistanceUtil.calculate(
                request.getLatitude(), request.getLongitude(),
                institution.getLbsLatitude(), institution.getLbsLongitude()
        );
        log.info("LBS距离计算: {}m（围栏半径: {}m）", distanceFromCenter, institution.getLbsRadius());

        // B3. 设备指纹生成与模拟器检测
        String deviceFingerprint = DeviceFingerprintUtil.generate(
                request.getDeviceBrand(), request.getDeviceModel(),
                request.getSystemVersion(), request.getWxVersion()
        );
        boolean emulatorSuspected = DeviceFingerprintUtil.isSuspectedEmulator(
                request.getDeviceBrand(), request.getDeviceModel(), request.getSystemVersion()
        );

        // B4. 同设备多账号检测（Redis 或 DB 查询，此处简化实现）
        boolean multiAccountSameDevice = checkMultiAccountSameDevice(deviceFingerprint, now);

        // B5. 学员频繁换设备检测
        boolean frequentDeviceChange = checkFrequentDeviceChange(
                request.getStudentId(), deviceFingerprint);

        // B6. 组装风控上下文，执行评分
        RiskAssessmentContext riskCtx = RiskAssessmentContext.builder()
                .checkInTime(now)
                .schedule(schedule)
                .distanceFromCenter(distanceFromCenter)
                .lbsRadius(institution.getLbsRadius())
                .emulatorSuspected(emulatorSuspected)
                .multiAccountSameDevice(multiAccountSameDevice)
                .frequentDeviceChange(frequentDeviceChange)
                .build();

        RiskAssessmentResult riskResult = RiskAssessmentEngine.assess(riskCtx);
        log.info("风控评估结果: score={}, level={}, details={}",
                riskResult.getRiskScore(), riskResult.getRiskLevel(),
                riskResult.getRiskDescription());

        // B7. 高危处理：直接阻断，等待教师确认
        if (riskResult.getRiskLevel() == CheckInResult.RiskLevel.HIGH_RISK) {
            // 高危时仍然插入一条待确认的签到记录（状态保持 PENDING，但记录风险信息）
            AttendanceLog pendingLog = buildAttendanceLog(request, schedule, institution,
                    classStudent.getId(), now, distanceFromCenter, deviceFingerprint,
                    riskResult, 3); // consumeResult=3 跳过
            attendanceLogMapper.insert(pendingLog);

            // 更新 class_student 风控标记（但仍不改为 CHECKED_IN）
            classStudent.setRiskFlag(1);
            classStudent.setRiskReason("高危签到需教师确认：" + riskResult.getRiskDescription());
            classStudentMapper.updateById(classStudent);

            log.warn("签到被风控拦截，等待教师确认: studentId={}, riskScore={}",
                    request.getStudentId(), riskResult.getRiskScore());

            return CheckInResult.highRisk(now, "QR_SCAN",
                    riskResult.getRiskScore(), riskResult.getRiskDescription(),
                    distanceFromCenter);
        }

        // ================================================================
        //  阶段 C：写入签到记录
        // ================================================================

        // C1. 更新点名册状态 → CHECKED_IN
        classStudent.setStatus("CHECKED_IN");
        classStudent.setCheckInTime(now);
        classStudent.setCheckInMethod("QR_SCAN");
        classStudent.setCheckInLat(request.getLatitude());
        classStudent.setCheckInLng(request.getLongitude());
        classStudent.setCheckInDevice(deviceFingerprint);
        classStudent.setRiskFlag(riskResult.getRiskScore() > 30 ? 1 : 0);
        classStudent.setRiskReason(riskResult.getRiskDescription());
        classStudentMapper.updateById(classStudent);

        // C2. 插入 attendance_log（审计日志）
        AttendanceLog attendanceLog = buildAttendanceLog(request, schedule, institution,
                classStudent.getId(), now, distanceFromCenter, deviceFingerprint,
                riskResult, 0); // consumeResult 先置 0，课消后回写
        attendanceLogMapper.insert(attendanceLog);
        log.info("签到记录已写入: attendanceLogId={}", attendanceLog.getId());

        // ================================================================
        //  阶段 D：自动课消（仅在 CHECKED_IN 状态触发）
        // ================================================================

        ConsumeResult consumeResult = null;
        try {
            ConsumeRequest consumeRequest = new ConsumeRequest();
            consumeRequest.setStudentId(request.getStudentId());
            consumeRequest.setCourseId(schedule.getCourseId());
            consumeRequest.setAmount(BigDecimal.ONE);  // 默认每次签到扣 1 课时
            consumeRequest.setBizType("ATTENDANCE");
            consumeRequest.setBizId(attendanceLog.getId());
            consumeRequest.setRemark("签到自动课消 - " + schedule.getScheduleDate());

            consumeResult = studentBalanceService.consumeHours(consumeRequest);

            // 回写 attendance_log 的课消结果
            if (consumeResult.getSuccess()) {
                attendanceLog.setConsumeResult(1);  // 扣减成功
                attendanceLog.setConsumeTransactionId(consumeResult.getTransactionId());
            } else {
                attendanceLog.setConsumeResult(2);  // 余额不足
            }
            attendanceLogMapper.updateById(attendanceLog);

        } catch (Exception e) {
            // 课消异常不阻塞签到流程：签到事实已经成立
            log.error("自动课消异常（签到已记录）: attendanceLogId={}", attendanceLog.getId(), e);
            attendanceLog.setConsumeResult(2);  // 标记余额不足
            attendanceLog.setConsumeTransactionId(null);
            attendanceLogMapper.updateById(attendanceLog);

            return CheckInResult.builder()
                    .checkInSuccess(true)
                    .checkInTime(now)
                    .checkInMethod("QR_SCAN")
                    .riskScore(riskResult.getRiskScore())
                    .riskLevel(riskResult.getRiskLevel())
                    .riskDescription(riskResult.getRiskDescription())
                    .distanceFromCenter(distanceFromCenter)
                    .consumptionTriggered(true)
                    .consumptionSuccess(false)
                    .consumptionFailReason("课消服务异常：" + e.getMessage())
                    .displayMessage("签到成功！课时扣减暂时失败，请联系老师处理")
                    .build();
        }

        // ================================================================
        //  阶段 E：组装最终返回结果
        // ================================================================

        log.info("==================== 签到流程完成 ====================");
        log.info("结果: checkIn=成功, riskScore={}, consumptionSuccess={}, remaining={}",
                riskResult.getRiskScore(),
                consumeResult != null ? consumeResult.getSuccess() : false,
                consumeResult != null ? consumeResult.getRemaining() : "N/A");

        return CheckInResult.success(now, "QR_SCAN",
                riskResult.getRiskScore(), riskResult.getRiskLevel(),
                distanceFromCenter, consumeResult);
    }

    // ================================================================
    //  教师手动签到
    // ================================================================

    /**
     * 教师手动签到 — 跳过二维码和 LBS 严格校验，但仍做基本的设备风控。
     * <p>适用场景：学员忘记带手机/手机没电时，由教师在教师端代为签到。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CheckInResult manualCheckIn(CheckInRequest request, Long teacherId) {
        log.info("教师手动签到: teacherId={}, scheduleId={}, studentId={}",
                teacherId, request.getScheduleId(), request.getStudentId());

        LocalDateTime now = LocalDateTime.now();

        // A1. 查询排课 + 校验教师是否有权限（简化，实际应查 teacher 表）
        ClassSchedule schedule = scheduleMapper.selectById(request.getScheduleId());
        if (schedule == null) {
            return CheckInResult.fail("该课程安排不存在");
        }

        // A2. 查询点名册
        ClassStudent classStudent = classStudentMapper.selectOne(
                new LambdaQueryWrapper<ClassStudent>()
                        .eq(ClassStudent::getScheduleId, request.getScheduleId())
                        .eq(ClassStudent::getStudentId, request.getStudentId())
        );
        if (classStudent == null) {
            return CheckInResult.fail("学员不在此课程名单中");
        }
        if (!"PENDING".equals(classStudent.getStatus())) {
            return CheckInResult.fail(
                    "LEAVE".equals(classStudent.getStatus())
                            ? "该学员已请假" : "该学员已签到");
        }

        // B. 更新点名册
        classStudent.setStatus("CHECKED_IN");
        classStudent.setCheckInTime(now);
        classStudent.setCheckInMethod("TEACHER_MANUAL");
        classStudentMapper.updateById(classStudent);

        // C. 写签到日志（风控分直接置 0，教师操作完全信任）
        AttendanceLog attendanceLog = AttendanceLog.builder()
                .institutionId(schedule.getInstitutionId())
                .scheduleId(request.getScheduleId())
                .classStudentId(classStudent.getId())
                .studentId(request.getStudentId())
                .checkInMethod("TEACHER_MANUAL")
                .checkInTime(now)
                .riskScore(0)
                .riskDetail("教师手动签到（teacherId=" + teacherId + "）")
                .consumeResult(0)
                .build();
        attendanceLogMapper.insert(attendanceLog);

        // D. 自动课消
        ConsumeResult consumeResult = null;
        try {
            ConsumeRequest consumeRequest = new ConsumeRequest();
            consumeRequest.setStudentId(request.getStudentId());
            consumeRequest.setCourseId(schedule.getCourseId());
            consumeRequest.setAmount(BigDecimal.ONE);
            consumeRequest.setBizType("ATTENDANCE");
            consumeRequest.setBizId(attendanceLog.getId());
            consumeRequest.setRemark("教师手动签到课消");

            consumeResult = studentBalanceService.consumeHours(consumeRequest);
            attendanceLog.setConsumeResult(consumeResult.getSuccess() ? 1 : 2);
            attendanceLog.setConsumeTransactionId(consumeResult.getTransactionId());
        } catch (Exception e) {
            log.error("教师手动签到-课消异常", e);
            attendanceLog.setConsumeResult(2);
        }
        attendanceLogMapper.updateById(attendanceLog);

        return CheckInResult.success(now, "TEACHER_MANUAL",
                0, CheckInResult.RiskLevel.SAFE, BigDecimal.ZERO, consumeResult);
    }

    // ================================================================
    //  私有方法：各项校验逻辑
    // ================================================================

    /**
     * 校验二维码 Token 的有效性。
     *
     * <p><b>校验规则：</b></p>
     * <ol>
     *   <li>排课记录的 qr_code_token 必须与请求中的 token 一致</li>
     *   <li>token 必须在有效期内（qr_code_expire_at > 当前时间）</li>
     * </ol>
     *
     * @param schedule 排课信息
     * @param token    前端传来的二维码 token
     * @param now      当前时间
     * @return null=校验通过, 非null=失败原因
     */
    private String validateQrToken(ClassSchedule schedule, String token, LocalDateTime now) {
        // 1. 排课未生成二维码
        if (schedule.getQrCodeToken() == null || schedule.getQrCodeToken().isEmpty()) {
            return "该课程尚未生成签到二维码";
        }

        // 2. Token 不匹配
        if (!schedule.getQrCodeToken().equals(token)) {
            log.warn("二维码Token不匹配: expected={}, actual={}",
                    schedule.getQrCodeToken(), token);
            return "签到码无效，请扫描正确的二维码";
        }

        // 3. Token 已过期
        if (schedule.getQrCodeExpireAt() != null
                && schedule.getQrCodeExpireAt().isBefore(now)) {
            log.warn("二维码Token已过期: expireAt={}, now={}",
                    schedule.getQrCodeExpireAt(), now);
            return "签到码已过期，请联系教师刷新二维码";
        }

        return null; // 校验通过
    }

    /**
     * 检测同一设备短时间内是否存在多账号签到。
     *
     * <p>策略：查询该设备指纹在过去 5 分钟内是否已有其他学员签到。
     * 使用 attendance_log 中的 device_info 字段进行匹配。</p>
     *
     * <p><b>生产优化：</b>此查询应走 Redis（Sorted Set 或 String），
     * 避免高频扫 MySQL。</p>
     */
    private boolean checkMultiAccountSameDevice(String deviceFingerprint, LocalDateTime now) {
        // 简化实现：查询近 5 分钟内相同设备指纹的不同学员签到记录
        try {
            Long count = attendanceLogMapper.selectCount(
                    new LambdaQueryWrapper<AttendanceLog>()
                            .eq(AttendanceLog::getDeviceInfo, deviceFingerprint)
                            // 注：这里只做设备匹配，不同学员的校验在 Java 层过滤；
                            // 生产环境建议用 Redis SET 存储 "deviceFingerprint → studentIds"
                            .ge(AttendanceLog::getCheckInTime, now.minusMinutes(5))
            );
            return count != null && count > 0;
            // 注：这里 count>0 意味着同一设备近期有签到记录，
            // 但不一定代表"异常"——比如同一手机换了账号。
            // 生产环境应进一步判断是否同一学员。
        } catch (Exception e) {
            log.warn("多账号设备检测查询失败（不影响主流程）", e);
            return false;
        }
    }

    /**
     * 检测学员近期是否频繁更换签到设备。
     *
     * <p>策略：查询该学员过去 7 天的签到记录中出现了几种不同的设备指纹。
     * 超过 3 种不同的设备则判定为频繁更换设备。</p>
     */
    private boolean checkFrequentDeviceChange(Long studentId, String currentFingerprint) {
        try {
            // 查询最近7天该学员的不同设备指纹数量
            // 生产环境建议用 Redis Bitmap 或 HyperLogLog 做高效去重统计
            // 这里简化实现：取最近20条记录，统计不同指纹数
            java.util.List<AttendanceLog> logs = attendanceLogMapper.selectList(
                    new LambdaQueryWrapper<AttendanceLog>()
                            .eq(AttendanceLog::getStudentId, studentId)
                            .ge(AttendanceLog::getCheckInTime, LocalDateTime.now().minusDays(7))
                            .orderByDesc(AttendanceLog::getCheckInTime)
                            .last("LIMIT 20")
            );

            if (logs == null || logs.isEmpty()) {
                return false; // 首次签到，不判定为频繁换设备
            }

            // 统计不同设备指纹数量
            long distinctDevices = logs.stream()
                    .map(AttendanceLog::getDeviceInfo)
                    .filter(f -> f != null && !f.isEmpty())
                    .distinct()
                    .count();

            // 如果当前设备不在历史记录中，+1
            boolean currentIsNew = logs.stream()
                    .noneMatch(l -> currentFingerprint.equals(l.getDeviceInfo()));
            if (currentIsNew) {
                distinctDevices++;
            }

            return distinctDevices > 3;
        } catch (Exception e) {
            log.warn("设备更换检测查询失败（不影响主流程）", e);
            return false;
        }
    }

    /**
     * 构建 attendance_log 实体。
     */
    private AttendanceLog buildAttendanceLog(CheckInRequest request,
                                              ClassSchedule schedule,
                                              Institution institution,
                                              Long classStudentId,
                                              LocalDateTime checkInTime,
                                              BigDecimal distanceFromCenter,
                                              String deviceFingerprint,
                                              RiskAssessmentResult riskResult,
                                              int consumeResult) {
        // 组装设备信息 JSON
        Map<String, String> deviceMap = new HashMap<>();
        deviceMap.put("brand", request.getDeviceBrand());
        deviceMap.put("model", request.getDeviceModel());
        deviceMap.put("systemVersion", request.getSystemVersion());
        deviceMap.put("wxVersion", request.getWxVersion());
        deviceMap.put("fingerprint", deviceFingerprint);

        return AttendanceLog.builder()
                .institutionId(schedule.getInstitutionId())
                .scheduleId(request.getScheduleId())
                .classStudentId(classStudentId)
                .studentId(request.getStudentId())
                .checkInMethod("QR_SCAN")
                .checkInTime(checkInTime)
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .distanceFromCenter(distanceFromCenter)
                .deviceInfo(JSONUtil.toJsonStr(deviceMap))
                .wxLocationVerify(1)   // 默认通过，后续接腾讯API后可更新
                .riskScore(riskResult.getRiskScore())
                .riskDetail(JSONUtil.toJsonStr(riskResult.getRiskDetails()))
                .consumeResult(consumeResult)
                .build();
    }
}
