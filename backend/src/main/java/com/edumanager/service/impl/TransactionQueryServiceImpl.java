package com.edumanager.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.edumanager.dto.CourseBalanceVO;
import com.edumanager.dto.TransactionVO;
import com.edumanager.entity.BalanceTransaction;
import com.edumanager.entity.Course;
import com.edumanager.entity.StudentBalance;
import com.edumanager.mapper.BalanceTransactionMapper;
import com.edumanager.mapper.CourseMapper;
import com.edumanager.mapper.StudentBalanceMapper;
import com.edumanager.service.TransactionQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 流水查询服务实现 — 纯读操作，不涉及事务修改
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionQueryServiceImpl implements TransactionQueryService {

    private final BalanceTransactionMapper transactionMapper;
    private final StudentBalanceMapper balanceMapper;
    private final CourseMapper courseMapper;

    @Override
    public List<TransactionVO> queryTransactions(Long studentId, Long courseId,
                                                  String changeType, int page, int pageSize) {
        // 构建查询条件
        LambdaQueryWrapper<BalanceTransaction> wrapper = new LambdaQueryWrapper<BalanceTransaction>()
                .eq(BalanceTransaction::getStudentId, studentId);

        if (courseId != null) {
            wrapper.eq(BalanceTransaction::getCourseId, courseId);
        }
        if (changeType != null && !changeType.isEmpty()) {
            wrapper.eq(BalanceTransaction::getChangeType, changeType.toUpperCase());
        }

        wrapper.orderByDesc(BalanceTransaction::getCreatedAt);

        // 分页查询
        Page<BalanceTransaction> mpPage = new Page<>(page, pageSize);
        Page<BalanceTransaction> result = transactionMapper.selectPage(mpPage, wrapper);

        if (result.getRecords().isEmpty()) {
            return Collections.emptyList();
        }

        // 批量查询课程名称（避免 N+1）
        List<Long> courseIds = result.getRecords().stream()
                .map(BalanceTransaction::getCourseId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> courseNameMap = courseMapper.selectBatchIds(courseIds).stream()
                .collect(Collectors.toMap(Course::getId, Course::getName));

        // 组装 VO
        return result.getRecords().stream()
                .map(tx -> TransactionVO.builder()
                        .id(tx.getId())
                        .changeType(tx.getChangeType())
                        .changeAmount(tx.getChangeAmount())
                        .balanceBefore(tx.getBalanceBefore())
                        .balanceAfter(tx.getBalanceAfter())
                        .courseName(courseNameMap.getOrDefault(tx.getCourseId(), "未知课程"))
                        .bizDescription(buildBizDescription(tx, courseNameMap))
                        .remark(tx.getRemark())
                        .createdAt(DateUtil.format(tx.getCreatedAt(), "MM-dd HH:mm"))
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<CourseBalanceVO> queryCourseBalances(Long studentId) {
        // 查询学员所有课程余额
        List<StudentBalance> balances = balanceMapper.selectList(
                new LambdaQueryWrapper<StudentBalance>()
                        .eq(StudentBalance::getStudentId, studentId)
                        .gt(StudentBalance::getRemaining, 0)  // 只查有剩余课时的
        );

        if (balances.isEmpty()) {
            return Collections.emptyList();
        }

        // 批量查课程信息
        List<Long> courseIds = balances.stream()
                .map(StudentBalance::getCourseId)
                .collect(Collectors.toList());
        Map<Long, Course> courseMap = courseMapper.selectBatchIds(courseIds).stream()
                .collect(Collectors.toMap(Course::getId, c -> c));

        LocalDate today = LocalDate.now();

        return balances.stream()
                .map(b -> {
                    Course c = courseMap.get(b.getCourseId());
                    int daysUntil = 999;
                    if (b.getExpiresAt() != null) {
                        daysUntil = (int) ChronoUnit.DAYS.between(today, b.getExpiresAt());
                    }
                    return CourseBalanceVO.builder()
                            .courseId(b.getCourseId())
                            .courseName(c != null ? c.getName() : "未知课程")
                            .coverUrl(c != null ? c.getCoverUrl() : null)
                            .category(c != null ? c.getCategory() : null)
                            .remaining(b.getRemaining())
                            .totalPurchased(b.getTotalPurchased())
                            .totalConsumed(b.getTotalConsumed())
                            .expiresAt(b.getExpiresAt() != null ? b.getExpiresAt().toString() : null)
                            .daysUntilExpiry(daysUntil)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * 构建业务描述文案
     */
    private String buildBizDescription(BalanceTransaction tx, Map<Long, String> nameMap) {
        String courseName = nameMap.getOrDefault(tx.getCourseId(), "未知课程");
        switch (tx.getChangeType()) {
            case "PURCHASE":
                return "购买课时包 — " + courseName;
            case "CONSUME":
                return "签到自动扣除 — " + courseName;
            case "REFUND":
                return "退费退还 — " + courseName;
            case "ADJUST":
                return "人工调整 — " + courseName;
            default:
                return courseName;
        }
    }
}
