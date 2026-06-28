package com.edumanager.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edumanager.dto.AddBalanceRequest;
import com.edumanager.dto.CreateOrderRequest;
import com.edumanager.dto.OrderResult;
import com.edumanager.entity.Course;
import com.edumanager.entity.CourseOrder;
import com.edumanager.entity.CoursePackage;
import com.edumanager.entity.StudentBalance;
import com.edumanager.exception.BusinessException;
import com.edumanager.mapper.CourseMapper;
import com.edumanager.mapper.CourseOrderMapper;
import com.edumanager.mapper.CoursePackageMapper;
import com.edumanager.service.CourseOrderService;
import com.edumanager.service.StudentBalanceService;
import com.edumanager.service.WxPayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 课程购买订单服务实现
 *
 * <p><b>幂等性保证：</b></p>
 * <ul>
 *   <li>创建订单前检查是否已有同 packageId + studentId 的未支付订单，有则复用</li>
 *   <li>支付回调处理前检查订单状态，已 PAID 则跳过（防止重复下发课时）</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CourseOrderServiceImpl implements CourseOrderService {

    private final CourseOrderMapper orderMapper;
    private final CoursePackageMapper packageMapper;
    private final CourseMapper courseMapper;
    private final WxPayService wxPayService;
    private final StudentBalanceService studentBalanceService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderResult createOrder(CreateOrderRequest request) {
        // 1. 查询课时包信息
        CoursePackage pkg = packageMapper.selectById(request.getPackageId());
        if (pkg == null || pkg.getStatus() == 0) {
            throw new BusinessException(20001, "该课时包已下架或不存在");
        }

        // 2. 查询课程信息
        Course course = courseMapper.selectById(pkg.getCourseId());
        if (course == null) {
            throw new BusinessException(20002, "课程信息异常");
        }

        // 3. 幂等检查：是否有未支付的同一套餐+学员订单（30分钟内）
        CourseOrder existingOrder = orderMapper.selectOne(
                new LambdaQueryWrapper<CourseOrder>()
                        .eq(CourseOrder::getStudentId, request.getStudentId())
                        .eq(CourseOrder::getPackageId, request.getPackageId())
                        .eq(CourseOrder::getStatus, "PENDING")
                        .ge(CourseOrder::getCreatedAt, LocalDateTime.now().minusMinutes(30))
        );

        CourseOrder order;
        if (existingOrder != null) {
            // 复用未支付订单（不重复创建，避免用户多次点击产生多个订单）
            log.info("复用未支付订单: orderNo={}", existingOrder.getOrderNo());
            order = existingOrder;
        } else {
            // 创建新订单
            String orderNo = generateOrderNo();
            order = CourseOrder.builder()
                    .orderNo(orderNo)
                    .institutionId(pkg.getInstitutionId())
                    .studentId(request.getStudentId())
                    .packageId(request.getPackageId())
                    .courseId(pkg.getCourseId())
                    .totalAmount(pkg.getPrice())
                    .paidAmount(pkg.getPrice())
                    .status("PENDING")
                    .build();
            orderMapper.insert(order);
            log.info("创建新订单: orderNo={}, amount={}分, studentId={}",
                    orderNo, pkg.getPrice(), request.getStudentId());
        }

        // 4. 调用微信支付统一下单
        String description = course.getName() + " - " + pkg.getName();
        Map<String, String> payParams = wxPayService.createJsapiOrder(
                order.getOrderNo(),
                request.getOpenid(),
                order.getTotalAmount(),
                description
        );

        // 5. 组装返回结果
        return OrderResult.builder()
                .orderNo(order.getOrderNo())
                .totalAmount(order.getTotalAmount())
                .courseName(course.getName())
                .packageName(pkg.getName())
                .totalHours(pkg.getTotalHours())
                .payParams(payParams)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handlePaymentNotify(String notifyJson) {
        log.info("处理微信支付回调: {}", notifyJson);

        // 1. 解析通知 JSON
        JSONObject notify = JSONUtil.parseObj(notifyJson);
        String orderNo = notify.getStr("out_trade_no");
        String transactionId = notify.getStr("transaction_id");
        String tradeState = notify.getStr("trade_state");

        if (orderNo == null) {
            log.error("支付回调缺少 out_trade_no");
            return;
        }

        // 2. 查询订单
        CourseOrder order = orderMapper.selectOne(
                new LambdaQueryWrapper<CourseOrder>()
                        .eq(CourseOrder::getOrderNo, orderNo)
        );
        if (order == null) {
            log.error("订单不存在: orderNo={}", orderNo);
            return;
        }

        // 3. 幂等判断：已支付则跳过
        if ("PAID".equals(order.getStatus())) {
            log.info("订单已处理，跳过: orderNo={}", orderNo);
            return;
        }

        // 4. 确认支付成功
        if (!"SUCCESS".equals(tradeState)) {
            log.warn("支付未成功: orderNo={}, tradeState={}", orderNo, tradeState);
            return;
        }

        // 5. 更新订单状态
        order.setStatus("PAID");
        order.setPayTime(LocalDateTime.now());
        order.setWxTransactionId(transactionId);
        orderMapper.updateById(order);
        log.info("订单支付成功: orderNo={}, transactionId={}", orderNo, transactionId);

        // 6. 下发课时到学员账户
        CoursePackage pkg = packageMapper.selectById(order.getPackageId());
        if (pkg == null) {
            log.error("课时包不存在，无法下发课时: packageId={}", order.getPackageId());
            return;
        }

        AddBalanceRequest addRequest = new AddBalanceRequest();
        addRequest.setStudentId(order.getStudentId());
        addRequest.setCourseId(order.getCourseId());
        addRequest.setAmount(new java.math.BigDecimal(pkg.getTotalHours()));
        addRequest.setOrderId(order.getId());
        addRequest.setRemark("购买" + pkg.getName() + " - 自动下发课时");

        try {
            studentBalanceService.addBalance(addRequest);
            log.info("课时下发成功: studentId={}, courseId={}, hours={}",
                    order.getStudentId(), order.getCourseId(), pkg.getTotalHours());
        } catch (Exception e) {
            // 课时下发失败 → 记录异常但不回滚订单状态，人工介入处理
            log.error("课时下发失败（需人工处理）: orderNo={}, studentId={}, courseId={}",
                    orderNo, order.getStudentId(), order.getCourseId(), e);
            // TODO: 写入异常工单表，通知运营人员手动下发
        }
    }

    @Override
    public String syncOrderStatus(String orderNo) {
        CourseOrder order = orderMapper.selectOne(
                new LambdaQueryWrapper<CourseOrder>()
                        .eq(CourseOrder::getOrderNo, orderNo)
        );
        if (order == null) {
            return "NOT_FOUND";
        }
        // 如果是 PENDING 状态，主动查询微信侧是否已支付
        if ("PENDING".equals(order.getStatus())) {
            String wxStatus = wxPayService.queryOrderStatus(orderNo);
            if ("SUCCESS".equals(wxStatus)) {
                // 微信侧已支付但回调丢失 → 手动触发处理
                log.warn("检测到支付回调丢失，手动补单: orderNo={}", orderNo);
                // 构造一个模拟通知 JSON 触发补单
                JSONObject mockNotify = new JSONObject();
                mockNotify.set("out_trade_no", orderNo);
                mockNotify.set("transaction_id", "MANUAL_SYNC_" + orderNo);
                mockNotify.set("trade_state", "SUCCESS");
                handlePaymentNotify(mockNotify.toString());
            }
            return wxStatus;
        }
        return order.getStatus();
    }

    /**
     * 生成订单号：时间戳 + 雪花ID后6位
     */
    private String generateOrderNo() {
        String snowflakeId = IdUtil.getSnowflakeNextIdStr();
        return "EDU" + snowflakeId.substring(snowflakeId.length() - 16);
    }
}
