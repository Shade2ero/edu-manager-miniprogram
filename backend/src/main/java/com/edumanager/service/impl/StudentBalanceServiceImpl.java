package com.edumanager.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edumanager.dto.AddBalanceRequest;
import com.edumanager.dto.ConsumeRequest;
import com.edumanager.dto.ConsumeResult;
import com.edumanager.entity.BalanceTransaction;
import com.edumanager.entity.StudentBalance;
import com.edumanager.exception.BusinessException;
import com.edumanager.mapper.BalanceTransactionMapper;
import com.edumanager.mapper.StudentBalanceMapper;
import com.edumanager.service.StudentBalanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * 学员课时账户服务实现
 *
 * <p><b>并发安全设计核心</b></p>
 * <p>扣减操作使用 <b>CAS（Compare And Swap）乐观锁</b> 实现：</p>
 * <pre>
 * UPDATE student_balance
 * SET remaining = remaining - #{amount},
 *     version = version + 1
 * WHERE id = #{balanceId}
 *   AND remaining >= #{amount}     ← 余额防线
 *   AND version = #{expectedVersion} ← 并发防线
 * </pre>
 *
 * <p>当受影响行数为 0 时，存在两种可能：</p>
 * <ol>
 *   <li>余额不足（remaining < amount）→ 直接返回失败</li>
 *   <li>版本号不匹配（被其他事务抢先修改）→ 重试</li>
 * </ol>
 * <p>区分方式：重试前先查询当前余额，不足则终止，否则 CAS 再试。</p>
 *
 * @see StudentBalanceMapper#consumeWithVersion 核心扣减 SQL
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StudentBalanceServiceImpl implements StudentBalanceService {

    /** 最大重试次数（乐观锁冲突时） */
    private static final int MAX_RETRY = 3;

    /** 余额预警阈值：剩余课时 <= 此值时触发续费提醒 */
    private static final BigDecimal LOW_BALANCE_THRESHOLD = new BigDecimal("2");

    private final StudentBalanceMapper balanceMapper;
    private final BalanceTransactionMapper transactionMapper;

    // ================================================================
    //  增加课时
    // ================================================================

    /**
     * 增加课时 — 购买课程后由支付成功回调触发。
     *
     * <p><b>业务规则：</b></p>
     * <ul>
     *   <li>如果账户不存在（首次购买），自动创建新账户</li>
     *   <li>如果账户已存在，累加课时并刷新到期日（取两者中较晚的）</li>
     *   <li>增加操作同样使用乐观锁，保证与扣减操作的互斥</li>
     * </ul>
     *
     * <p><b>幂等性：</b>调用方应通过订单号去重，避免重复加课时。</p>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public StudentBalance addBalance(AddBalanceRequest request) {
        log.info("增加课时: studentId={}, courseId={}, amount={}, orderId={}",
                request.getStudentId(), request.getCourseId(),
                request.getAmount(), request.getOrderId());

        // 1. 幂等校验：检查该订单是否已经加过课时
        long count = transactionMapper.selectCount(
                new LambdaQueryWrapper<BalanceTransaction>()
                        .eq(BalanceTransaction::getBizType, "ORDER")
                        .eq(BalanceTransaction::getBizId, request.getOrderId())
        );
        if (count > 0) {
            log.warn("订单 {} 已处理过课时充值，跳过重复执行", request.getOrderId());
            // 返回现有账户
            return getBalanceOrThrow(request.getStudentId(), request.getCourseId());
        }

        // 2. 查询或创建账户
        StudentBalance balance = getOrCreateBalance(
                request.getStudentId(), request.getCourseId(), request.getAmount());

        // 3. 乐观锁增加课时
        int affected = balanceMapper.addWithVersion(
                balance.getId(), request.getAmount(), balance.getVersion());

        if (affected == 0) {
            // 乐观锁冲突 → 重试一次（增加操作冲突概率低）
            log.warn("增加课时乐观锁冲突，重试中... balanceId={}", balance.getId());
            StudentBalance retryBalance = getBalanceOrThrow(
                    request.getStudentId(), request.getCourseId());
            affected = balanceMapper.addWithVersion(
                    retryBalance.getId(), request.getAmount(), retryBalance.getVersion());

            if (affected == 0) {
                throw BusinessException.concurrentConflict();
            }
            balance = retryBalance;
        }

        // 4. 刷新到期日（取最晚的）
        LocalDate newExpiry = request.getAmount() != null
                ? LocalDate.now().plusDays(365)  // 简化：默认365天有效期
                : balance.getExpiresAt();
        if (balance.getExpiresAt() == null || newExpiry.isAfter(balance.getExpiresAt())) {
            balance.setExpiresAt(newExpiry);
        }

        // 5. 重新加载最新数据
        balance = balanceMapper.selectById(balance.getId());

        // 6. 写入课时流水
        BalanceTransaction tx = BalanceTransaction.builder()
                .institutionId(balance.getInstitutionId())
                .studentId(request.getStudentId())
                .courseId(request.getCourseId())
                .balanceId(balance.getId())
                .changeType("PURCHASE")
                .changeAmount(request.getAmount())
                .balanceBefore(balance.getRemaining().subtract(request.getAmount()))
                .balanceAfter(balance.getRemaining())
                .bizType("ORDER")
                .bizId(request.getOrderId())
                .remark(request.getRemark())
                .build();
        transactionMapper.insert(tx);

        log.info("增加课时成功: balanceId={}, amount={}, remaining={}",
                balance.getId(), request.getAmount(), balance.getRemaining());
        return balance;
    }

    // ================================================================
    //  安全扣减课时（核心方法）
    // ================================================================

    /**
     * 安全扣减课时 — 使用乐观锁 CAS + 重试机制保证并发安全。
     *
     * <p><b>执行流程（详细）：</b></p>
     * <ol>
     *   <li><b>参数校验</b> — amount <= 0 直接拒</li>
     *   <li><b>账户存在性检查</b> — 查不到就是没买过课</li>
     *   <li><b>CAS 扣减循环（最多 {@value MAX_RETRY} 次）</b>
     *     <ul>
     *       <li>执行 consumeWithVersion SQL</li>
     *       <li>affected=1 → 扣减成功，跳出循环</li>
     *       <li>affected=0 → 查余额：不足则抛异常，否则自旋重试</li>
     *     </ul>
     *   </li>
     *   <li><b>写入课时流水</b> — balance_transaction 记录本次消耗</li>
     *   <li><b>余额预警</b> — remaining <= 2 课时时标记预警</li>
     *   <li><b>组装 ConsumeResult</b> 返回</li>
     * </ol>
     *
     * <p><b>为什么要用显式 CAS 而不是 MyBatis-Plus 的 @Version？</b></p>
     * <p>MyBatis-Plus 的 @Version 只在 updateById 时生效，且无法在 SQL 层面
     * 加 remaining >= amount 的业务约束。CAS 手写 SQL 将"版本校验"和"余额校验"
     * 合并在一个原子操作中，避免间隙。</p>
     *
     * @param request 扣减请求
     * @return 扣减结果（含成功标志、剩余课时、流水ID、预警标记）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ConsumeResult consumeHours(ConsumeRequest request) {
        log.info("开始扣减课时: studentId={}, courseId={}, amount={}, bizType={}, bizId={}",
                request.getStudentId(), request.getCourseId(),
                request.getAmount(), request.getBizType(), request.getBizId());

        // ===== 阶段1：参数校验 =====
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw BusinessException.illegalAmount();
        }

        // ===== 阶段2：查询账户是否存在 =====
        StudentBalance balance = getBalance(request.getStudentId(), request.getCourseId());
        if (balance == null) {
            log.warn("学员 {} 课程 {} 课时账户不存在", request.getStudentId(), request.getCourseId());
            return ConsumeResult.accountNotFound();
        }

        // 保存扣减前余额（用于写流水）
        BigDecimal balanceBefore = balance.getRemaining();

        // ===== 阶段3：CAS 乐观锁扣减（最多重试 MAX_RETRY 次） =====
        int affected = 0;
        int retryCount = 0;

        while (retryCount < MAX_RETRY) {
            // 3.1 执行 CAS 扣减 SQL
            affected = balanceMapper.consumeWithVersion(
                    balance.getId(),
                    request.getAmount(),
                    balance.getVersion()   // ← 期望版本号
            );

            if (affected == 1) {
                // 扣减成功！退出重试循环
                log.info("CAS扣减成功: balanceId={}, amount={}, retry={}",
                        balance.getId(), request.getAmount(), retryCount);
                break;
            }

            // 3.2 affected=0 → 要么余额不足，要么版本冲突
            retryCount++;
            log.warn("CAS扣减失败(第{}次): affected=0, balanceId={}, version={}",
                    retryCount, balance.getId(), balance.getVersion());

            // 3.3 重新读取最新数据，区分原因
            StudentBalance latest = balanceMapper.selectById(balance.getId());

            if (latest == null) {
                throw BusinessException.concurrentConflict();
            }

            // 3.4 判断是否是余额不足
            if (latest.getRemaining().compareTo(request.getAmount()) < 0) {
                // 确实没课时了 → 终止重试，返回余额不足
                log.warn("课时余额不足: studentId={}, courseId={}, remaining={}, need={}",
                        request.getStudentId(), request.getCourseId(),
                        latest.getRemaining(), request.getAmount());
                return ConsumeResult.insufficient(latest.getRemaining());
            }

            // 3.5 余额够但版本变了 → 是并发冲突，用新版本号再试一次
            balance = latest;
        }

        // 3.6 重试次数耗尽仍然失败
        if (affected == 0) {
            log.error("CAS扣减失败：重试{}次后仍未成功, balanceId={}", MAX_RETRY, balance.getId());
            throw BusinessException.concurrentConflict();
        }

        // ===== 阶段4：重新加载扣减后的最新数据 =====
        balance = balanceMapper.selectById(balance.getId());

        // ===== 阶段5：写入课时流水（审计用，只追加不修改） =====
        BalanceTransaction tx = BalanceTransaction.builder()
                .institutionId(balance.getInstitutionId())
                .studentId(request.getStudentId())
                .courseId(request.getCourseId())
                .balanceId(balance.getId())
                .changeType("CONSUME")
                .changeAmount(request.getAmount().negate())   // 负数表示消耗
                .balanceBefore(balanceBefore)
                .balanceAfter(balance.getRemaining())
                .bizType(request.getBizType())
                .bizId(request.getBizId())
                .remark(request.getRemark())
                .build();
        transactionMapper.insert(tx);

        // ===== 阶段6：余额预警检查 =====
        boolean lowBalance = balance.getRemaining().compareTo(LOW_BALANCE_THRESHOLD) <= 0;
        if (lowBalance) {
            log.warn("课时余额预警: studentId={}, courseId={}, remaining={}",
                    request.getStudentId(), request.getCourseId(), balance.getRemaining());
            // 此处可集成消息推送（见 LowBalanceEvent 监听器）
        }

        log.info("扣减课时完成: studentId={}, courseId={}, amount={}, remaining={}, txId={}",
                request.getStudentId(), request.getCourseId(),
                request.getAmount(), balance.getRemaining(), tx.getId());

        // ===== 阶段7：返回结果 =====
        return ConsumeResult.success(
                balanceBefore,
                balance.getRemaining(),
                request.getAmount(),
                tx.getId(),
                lowBalance
        );
    }

    // ================================================================
    //  查询余额
    // ================================================================

    @Override
    public StudentBalance getBalance(Long studentId, Long courseId) {
        return balanceMapper.selectOne(
                new LambdaQueryWrapper<StudentBalance>()
                        .eq(StudentBalance::getStudentId, studentId)
                        .eq(StudentBalance::getCourseId, courseId)
        );
    }

    // ================================================================
    //  私有辅助方法
    // ================================================================

    /**
     * 查询账户，不存在时抛业务异常。
     */
    private StudentBalance getBalanceOrThrow(Long studentId, Long courseId) {
        StudentBalance balance = getBalance(studentId, courseId);
        if (balance == null) {
            throw BusinessException.balanceNotFound(studentId, courseId);
        }
        return balance;
    }

    /**
     * 查询或创建账户。
     * <p>
     * 相当于 upsert 逻辑：如果已存在则返回现有记录，
     * 否则新建一条 remaining = initialAmount 的账户记录。
     * </p>
     * <p>
     * <b>注意：</b>这里不用 INSERT ... ON DUPLICATE KEY UPDATE，
     * 因为首次创建不需要"原子性"那么强——万一并发创建两次，
     * MySQL 唯一索引 uk_student_course 会拒绝第二条 INSERT，
     * 此时回退到 SELECT 即可。
     * </p>
     */
    private StudentBalance getOrCreateBalance(Long studentId, Long courseId, BigDecimal initialAmount) {
        StudentBalance existing = getBalance(studentId, courseId);
        if (existing != null) {
            return existing;
        }

        // 创建新账户
        StudentBalance newBalance = StudentBalance.builder()
                .studentId(studentId)
                .courseId(courseId)
                .institutionId(1L)  // TODO: 从课程或请求中获取 institutionId
                .totalPurchased(BigDecimal.ZERO)
                .totalConsumed(BigDecimal.ZERO)
                .remaining(BigDecimal.ZERO)
                .version(0)
                .build();

        try {
            balanceMapper.insert(newBalance);
            log.info("创建新的课时账户: studentId={}, courseId={}, balanceId={}",
                    studentId, courseId, newBalance.getId());
            return newBalance;
        } catch (Exception e) {
            // 唯一索引冲突 → 并发创建，回退到查询
            log.warn("账户创建冲突（并发），回退查询: studentId={}, courseId={}", studentId, courseId);
            return getBalanceOrThrow(studentId, courseId);
        }
    }
}
