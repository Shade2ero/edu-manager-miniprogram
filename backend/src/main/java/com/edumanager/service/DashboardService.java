package com.edumanager.service;

import com.edumanager.dto.DashboardVO;

/**
 * 数据看板服务
 */
public interface DashboardService {

    /**
     * 获取机构本月数据看板
     *
     * @param institutionId 机构ID
     * @return 看板统计数据
     */
    DashboardVO getMonthlyDashboard(Long institutionId);
}
