package com.edumanager.controller;

import com.edumanager.common.Result;
import com.edumanager.dto.CourseBalanceVO;
import com.edumanager.dto.TransactionVO;
import com.edumanager.service.TransactionQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 课时流水与余额查询 Controller
 *
 * <p>注意：单个课程余额查询由 StudentBalanceController 的 GET /api/balance 处理。
 * 本 Controller 仅负责流水列表和多课程余额列表。</p>
 */
@RestController
@RequestMapping("/api/balance")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionQueryService transactionQueryService;

    /**
     * 查询学员所有课程的课时余额列表
     *
     * <pre>
     * GET /api/balance/list?studentId=1
     * </pre>
     *
     * <p>返回该学员购买过的所有课程及其课时余额</p>
     */
    @GetMapping("/list")
    public Result<List<CourseBalanceVO>> listBalances(@RequestParam Long studentId) {
        List<CourseBalanceVO> list = transactionQueryService.queryCourseBalances(studentId);
        return Result.ok(list);
    }

    /**
     * 查询课时流水列表
     *
     * <pre>
     * GET /api/balance/transactions?studentId=1&courseId=1&changeType=CONSUME&page=1&pageSize=20
     * </pre>
     *
     * @param studentId  学员ID（必填）
     * @param courseId   课程ID（可选，不传查全部课程）
     * @param changeType 类型过滤（可选：PURCHASE/CONSUME/REFUND）
     * @param page       页码，默认1
     * @param pageSize   每页条数，默认20
     */
    @GetMapping("/transactions")
    public Result<List<TransactionVO>> listTransactions(
            @RequestParam Long studentId,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) String changeType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        List<TransactionVO> list = transactionQueryService.queryTransactions(
                studentId, courseId, changeType, page, pageSize);
        return Result.ok(list);
    }
}
