package com.edumanager.controller;

import com.edumanager.common.Result;
import com.edumanager.dto.AddBalanceRequest;
import com.edumanager.dto.ConsumeRequest;
import com.edumanager.dto.ConsumeResult;
import com.edumanager.entity.StudentBalance;
import com.edumanager.service.StudentBalanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 学员课时账户 Controller
 * <p>提供课时查询、增加、扣减的 REST API</p>
 */
@RestController
@RequestMapping("/api/balance")
@RequiredArgsConstructor
@Validated
public class StudentBalanceController {

    private final StudentBalanceService balanceService;

    /**
     * 查询学员某课程的课时余额
     *
     * <pre>
     * GET /api/balance?studentId=1&courseId=1
     * </pre>
     */
    @GetMapping
    public Result<StudentBalance> getBalance(@RequestParam Long studentId,
                                              @RequestParam Long courseId) {
        StudentBalance balance = balanceService.getBalance(studentId, courseId);
        if (balance == null) {
            return Result.fail(10002, "课时账户不存在，请先购买课程");
        }
        return Result.ok(balance);
    }

    /**
     * 增加课时（购买课程后由支付回调调用）
     *
     * <pre>
     * POST /api/balance/add
     * Body: { "studentId": 1, "courseId": 1, "amount": 16, "orderId": 123 }
     * </pre>
     */
    @PostMapping("/add")
    public Result<StudentBalance> addBalance(@Valid @RequestBody AddBalanceRequest request) {
        StudentBalance balance = balanceService.addBalance(request);
        return Result.ok(balance);
    }

    /**
     * 扣减课时（签到后由课消逻辑调用）
     *
     * <pre>
     * POST /api/balance/consume
     * Body: { "studentId": 1, "courseId": 1, "amount": 1, "bizType": "ATTENDANCE", "bizId": 456 }
     * </pre>
     *
     * <p>返回 ConsumeResult：
     * <ul>
     *   <li>success=true → 扣减成功，remaining 为剩余课时</li>
     *   <li>success=false → 扣减失败，failReason 说明原因</li>
     * </ul>
     * </p>
     */
    @PostMapping("/consume")
    public Result<ConsumeResult> consumeHours(@Valid @RequestBody ConsumeRequest request) {
        ConsumeResult result = balanceService.consumeHours(request);
        return Result.ok(result);
    }
}
