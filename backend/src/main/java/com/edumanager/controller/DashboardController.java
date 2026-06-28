package com.edumanager.controller;

import com.edumanager.common.Result;
import com.edumanager.dto.DashboardVO;
import com.edumanager.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 数据看板 Controller
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * 获取机构月度数据看板
     *
     * <pre>
     * GET /api/dashboard?institutionId=1
     * </pre>
     *
     * <p>返回：考勤率、课消率、近7天趋势、预警数据等</p>
     */
    @GetMapping
    public Result<DashboardVO> getDashboard(@RequestParam Long institutionId) {
        return Result.ok(dashboardService.getMonthlyDashboard(institutionId));
    }
}
