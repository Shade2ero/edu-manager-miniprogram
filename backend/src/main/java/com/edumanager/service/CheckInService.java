package com.edumanager.service;

import com.edumanager.dto.CheckInRequest;
import com.edumanager.dto.CheckInResult;

/**
 * 签到服务接口
 *
 * <p><b>完整处理流程：</b></p>
 * <ol>
 *   <li><b>身份核验</b> — 二维码 Token 有效性 & 排课-学员归属关系</li>
 *   <li><b>防作弊校验</b> — 时间窗口 / LBS 围栏 / 设备指纹 / 重复签到检测</li>
 *   <li><b>风控评分</b> — 累积评分，分级处理（安全/可疑/警告/高危）</li>
 *   <li><b>写入签到</b> — 更新 class_student 状态 + 写入 attendance_log</li>
 *   <li><b>自动课消</b> — 状态为 CHECKED_IN 时调用 StudentBalanceService.consumeHours()</li>
 *   <li><b>返回结果</b> — 签到状态 + 风控结果 + 课消结果</li>
 * </ol>
 */
public interface CheckInService {

    /**
     * 处理学生签到请求（一站式：校验 → 风控 → 签到 → 课消）
     *
     * @param request 签到请求（排课ID、学员ID、二维码token、位置、设备信息）
     * @return 签到结果（含风控等级和课消状态）
     */
    CheckInResult checkIn(CheckInRequest request);

    /**
     * 教师手动签到（跳过二维码和 LBS 校验，但仍做设备风控）
     *
     * @param request    签到请求（教师端提交时 qrToken 可为空）
     * @param teacherId  操作教师ID（用于审计）
     * @return 签到结果
     */
    CheckInResult manualCheckIn(CheckInRequest request, Long teacherId);
}
