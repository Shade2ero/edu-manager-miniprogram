package com.edumanager.service;

import com.edumanager.dto.CourseBalanceVO;
import com.edumanager.dto.TransactionVO;

import java.util.List;

/**
 * 课时流水与余额查询服务
 */
public interface TransactionQueryService {

    /**
     * 查询学员某课程的课时流水列表
     *
     * @param studentId 学员ID
     * @param courseId  课程ID（可选，不传则查所有课程）
     * @param changeType 变动类型过滤（可选：PURCHASE/CONSUME/REFUND）
     * @param page      页码（从1开始）
     * @param pageSize  每页条数
     * @return 流水列表
     */
    List<TransactionVO> queryTransactions(Long studentId, Long courseId,
                                           String changeType, int page, int pageSize);

    /**
     * 查询学员所有课程的课时余额列表
     *
     * @param studentId 学员ID
     * @return 各课程的余额信息
     */
    List<CourseBalanceVO> queryCourseBalances(Long studentId);
}
