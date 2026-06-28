package com.edumanager.controller;

import com.edumanager.common.Result;
import com.edumanager.dto.CheckInRequest;
import com.edumanager.dto.CheckInResult;
import com.edumanager.service.CheckInService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

/**
 * 签到 Controller
 *
 * <p>提供学生扫码签到、LBS 签到、教师手动签到的 REST API。</p>
 *
 * <p><b>接口列表：</b></p>
 * <table>
 *   <tr><th>方法</th><th>路径</th><th>说明</th></tr>
 *   <tr><td>POST</td><td>/api/checkin/scan</td><td>学生扫码签到（主接口）</td></tr>
 *   <tr><td>POST</td><td>/api/checkin/lbs</td><td>学生 LBS 定位签到</td></tr>
 *   <tr><td>POST</td><td>/api/checkin/manual</td><td>教师手动签到</td></tr>
 *   <tr><td>GET</td><td>/api/checkin/status</td><td>查询签到状态</td></tr>
 * </table>
 */
@Slf4j
@RestController
@RequestMapping("/api/checkin")
@RequiredArgsConstructor
@Validated
public class CheckInController {

    private final CheckInService checkInService;

    /**
     * 【主接口】学生扫码签到
     *
     * <p><b>前端调用流程：</b></p>
     * <ol>
     *   <li>调用 wx.scanCode() 扫描教师端展示的二维码</li>
     *   <li>从二维码中解析出 scheduleId 和 qrToken</li>
     *   <li>调用 wx.getLocation() 获取当前位置</li>
     *   <li>调用 wx.getDeviceInfo() / wx.getAppBaseInfo() 获取设备信息</li>
     *   <li>组装参数调用本接口</li>
     * </ol>
     *
     * <pre>
     * POST /api/checkin/scan
     * Body: {
     *   "scheduleId": 123,
     *   "studentId": 456,
     *   "qrToken": "uuid-token-from-qr",
     *   "latitude": 31.2304,
     *   "longitude": 121.4737,
     *   "deviceBrand": "Apple",
     *   "deviceModel": "iPhone 15 Pro",
     *   "systemVersion": "iOS 17.4",
     *   "wxVersion": "8.0.48"
     * }
     * </pre>
     *
     * <p><b>返回结果解读：</b></p>
     * <ul>
     *   <li>checkInSuccess=true + riskLevel=SAFE → 签到成功，正常显示剩余课时</li>
     *   <li>checkInSuccess=true + riskLevel=SUSPICIOUS/WARNING → 签到成功但后台标记</li>
     *   <li>checkInSuccess=false + riskLevel=HIGH_RISK → 需要教师确认</li>
     *   <li>checkInSuccess=false + displayMessage → 签到失败，显示提示信息</li>
     * </ul>
     */
    @PostMapping("/scan")
    public Result<CheckInResult> scanCheckIn(@Valid @RequestBody CheckInRequest request,
                                              HttpServletRequest httpRequest) {
        log.info("收到扫码签到请求: studentId={}, scheduleId={}",
                request.getStudentId(), request.getScheduleId());

        // 从请求中获取客户端真实 IP（用于审计）
        String clientIp = getClientIp(httpRequest);

        // 设置签到方式
        request.setQrToken(request.getQrToken() != null ? request.getQrToken() : "");

        CheckInResult result = checkInService.checkIn(request);
        return Result.ok(result);
    }

    /**
     * 学生 LBS 定位签到（无需扫码，纯地理位置签到）
     *
     * <p>与扫码签到的区别：不传 qrToken，风控更严格（LBS 权重更高）。</p>
     * <p>适用场景：机构开启了"到店自动签到"功能。</p>
     */
    @PostMapping("/lbs")
    public Result<CheckInResult> lbsCheckIn(@Valid @RequestBody CheckInRequest request,
                                             HttpServletRequest httpRequest) {
        log.info("收到LBS签到请求: studentId={}, scheduleId={}",
                request.getStudentId(), request.getScheduleId());

        // LBS 签到不依赖二维码 token
        request.setQrToken(null);

        CheckInResult result = checkInService.checkIn(request);
        return Result.ok(result);
    }

    /**
     * 教师手动签到
     *
     * <p>教师在教师端为无法正常签到的学员代为签到。</p>
     * <p>需要传 teacherId 参数用于审计，风控分直接置 0。</p>
     */
    @PostMapping("/manual")
    public Result<CheckInResult> manualCheckIn(@Valid @RequestBody CheckInRequest request,
                                                @RequestParam Long teacherId) {
        log.info("收到教师手动签到请求: teacherId={}, studentId={}, scheduleId={}",
                teacherId, request.getStudentId(), request.getScheduleId());

        request.setQrToken(null);
        CheckInResult result = checkInService.manualCheckIn(request, teacherId);
        return Result.ok(result);
    }

    /**
     * 查询学员在某节课的签到状态
     *
     * <pre>
     * GET /api/checkin/status?scheduleId=123&studentId=456
     * </pre>
     *
     * <p>返回该学员在点名册中的状态（PENDING / CHECKED_IN / ABSENT / LEAVE）</p>
     */
    @GetMapping("/status")
    public Result<String> getCheckInStatus(@RequestParam Long scheduleId,
                                            @RequestParam Long studentId) {
        // 简化实现：实际应调用 Service 查询
        // 此处仅作为接口占位，完整实现见后续迭代
        return Result.ok("PENDING");
    }

    // ================================================================
    //  私有工具方法
    // ================================================================

    /**
     * 获取客户端真实 IP（考虑反向代理场景）
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多级代理时取第一个 IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
