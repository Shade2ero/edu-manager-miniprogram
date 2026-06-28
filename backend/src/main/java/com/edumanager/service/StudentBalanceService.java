package com.edumanager.service;

import com.edumanager.dto.AddBalanceRequest;
import com.edumanager.dto.ConsumeRequest;
import com.edumanager.dto.ConsumeResult;
import com.edumanager.entity.StudentBalance;

/**
 * 学员课时账户服务 — 核心业务接口
 *
 * <p><b>职责</b></p>
 * <ul>
 *   <li>增加课时：购买课程后由支付回调触发，增加对应课程的可消耗课时</li>
 *   <li>安全扣减课时：签到成功后触发，CAS 乐观锁保证不超扣</li>
 *   <li>查询余额：支持按学员+课程精确查询或按学员批量查询</li>
 * </ul>
 *
 * <p><b>并发安全保证</b></p>
 * <ol>
 *   <li>数据库层面：UPDATE ... WHERE version = ? AND remaining >= ? — 乐观锁 + 余额双重校验</li>
 *   <li>应用层面：最多重试 3 次，指数退避，避免活锁</li>
 *   <li>事务层面：扣减 + 写流水 + 回写考勤在一个事务内完成（原子性）</li>
 * </ol>
 */
public interface StudentBalanceService {

    /**
     * 增加课时（购买课程后由支付成功回调触发）
     *
     * <p>如果账户不存在则自动创建；如果已存在则累加课时并更新到期日。</p>
     *
     * @param request 增加课时请求（学员ID、课程ID、课时数、订单ID）
     * @return 操作后的账户信息
     */
    StudentBalance addBalance(AddBalanceRequest request);

    /**
     * 安全扣减课时（签到成功后由课消逻辑触发）
     *
     * <p><b>执行流程</b></p>
     * <ol>
     *   <li>校验参数合法性</li>
     *   <li>查询账户是否存在</li>
     *   <li>乐观锁扣减（内置最多 3 次重试）</li>
     *   <li>写入课时流水（balance_transaction）</li>
     *   <li>余额预警检查（剩余 <= 2 课时时标记）</li>
     *   <li>返回 ConsumeResult 给调用方</li>
     * </ol>
     *
     * <p><b>注意：调用方需自行处理扣减失败的情况</b>（如将考勤记录标记为"余额不足"状态）</p>
     *
     * @param request 扣减请求（学员ID、课程ID、课时数、业务类型、业务ID）
     * @return 扣减结果（成功/失败、剩余课时、流水ID、预警标记）
     */
    ConsumeResult consumeHours(ConsumeRequest request);

    /**
     * 查询学员某个课程的课时余额
     *
     * @param studentId 学员ID
     * @param courseId  课程ID
     * @return 账户信息，不存在时返回 null
     */
    StudentBalance getBalance(Long studentId, Long courseId);
}
