package com.edumanager.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edumanager.common.Result;
import com.edumanager.dto.CreateCourseRequest;
import com.edumanager.entity.ClassSchedule;
import com.edumanager.entity.Course;
import com.edumanager.entity.CoursePackage;
import com.edumanager.entity.StudentBalance;
import com.edumanager.mapper.ClassScheduleMapper;
import com.edumanager.mapper.CourseMapper;
import com.edumanager.mapper.CoursePackageMapper;
import com.edumanager.mapper.StudentBalanceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 课程与课时包展示 Controller
 *
 * <p>提供课程列表、课时包详情等公开查询接口</p>
 */
@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseMapper courseMapper;
    private final CoursePackageMapper packageMapper;
    private final ClassScheduleMapper scheduleMapper;
    private final StudentBalanceMapper balanceMapper;

    /**
     * 获取机构所有上架的课程列表（含课时包）
     *
     * <pre>
     * GET /api/courses?institutionId=1
     * </pre>
     */
    @GetMapping
    public Result<List<Map<String, Object>>> listCourses(
            @RequestParam(defaultValue = "1") Long institutionId,
            @RequestParam(defaultValue = "false") boolean admin) {
        // 查课程（管理员模式显示全部含下架）
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<Course>()
                .eq(Course::getInstitutionId, institutionId);
        if (!admin) wrapper.eq(Course::getStatus, 1);
        List<Course> courses = courseMapper.selectList(wrapper);

        // 查每个课程下的课时包
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (Course c : courses) {
            List<CoursePackage> packages = packageMapper.selectList(
                    new LambdaQueryWrapper<CoursePackage>()
                            .eq(CoursePackage::getCourseId, c.getId())
                            .eq(CoursePackage::getStatus, 1)
                            .orderByAsc(CoursePackage::getSortOrder)
            );

            Map<String, Object> item = new HashMap<>();
            item.put("courseId", c.getId());
            item.put("courseName", c.getName());
            item.put("coverUrl", c.getCoverUrl());
            item.put("description", c.getDescription());
            item.put("category", c.getCategory());
            item.put("defaultDuration", c.getDefaultDuration());
            item.put("status", c.getStatus());
            item.put("updatedAt", c.getUpdatedAt() != null ? c.getUpdatedAt().toString() : null);
            item.put("packages", packages);
            result.add(item);
        }
        return Result.ok(result);
    }

    /**
     * 获取某课程详情及其课时包
     *
     * <pre>
     * GET /api/courses/1
     * </pre>
     */
    @GetMapping("/{courseId}")
    public Result<Map<String, Object>> getCourseDetail(@PathVariable Long courseId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null) {
            return Result.fail(404, "课程不存在");
        }

        List<CoursePackage> packages = packageMapper.selectList(
                new LambdaQueryWrapper<CoursePackage>()
                        .eq(CoursePackage::getCourseId, courseId)
                        .eq(CoursePackage::getStatus, 1)
                        .orderByAsc(CoursePackage::getSortOrder)
        );

        Map<String, Object> result = new HashMap<>();
        result.put("course", course);
        result.put("packages", packages);
        return Result.ok(result);
    }

    /**
     * 管理员：切换课程上下架状态
     * <pre>PUT /api/courses/1/toggle</pre>
     */
    @PutMapping("/{courseId}/toggle")
    public Result<String> toggleCourseStatus(@PathVariable Long courseId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null) return Result.fail(404, "课程不存在");
        course.setStatus(course.getStatus() == 1 ? 0 : 1);
        courseMapper.updateById(course);
        return Result.ok(course.getStatus() == 1 ? "已上架" : "已下架");
    }

    /**
     * 管理员：新增课程
     * <pre>POST /api/courses?institutionId=1</pre>
     */
    @PostMapping
    public Result<Map<String, Object>> createCourse(@RequestParam Long institutionId,
                                                     @Valid @RequestBody CreateCourseRequest req) {
        Course c = new Course();
        c.setInstitutionId(institutionId);
        c.setName(req.getName());
        c.setDescription(req.getDescription());
        c.setCategory(req.getCategory());
        c.setDefaultDuration(req.getDefaultDuration());
        c.setStatus(1);
        courseMapper.insert(c);

        // 创建课时包
        if (req.getPackages() != null) {
            for (CreateCourseRequest.PackageItem pi : req.getPackages()) {
                CoursePackage pkg = new CoursePackage();
                pkg.setInstitutionId(institutionId);
                pkg.setCourseId(c.getId());
                pkg.setName(pi.getName());
                pkg.setTotalHours(pi.getTotalHours());
                pkg.setPrice(pi.getPrice());
                pkg.setOriginalPrice(pi.getOriginalPrice());
                pkg.setValidDays(pi.getValidDays() != null ? pi.getValidDays() : 365);
                pkg.setStatus(1);
                packageMapper.insert(pkg);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("courseId", c.getId());
        result.put("courseName", c.getName());
        return Result.ok(result);
    }

    /**
     * 管理员：删除课程（全链路校验：排课 → 学员课时 → 订单，全部清空后才可删除）
     * <pre>DELETE /api/courses/1</pre>
     */
    @DeleteMapping("/{courseId}")
    public Result<String> deleteCourse(@PathVariable Long courseId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null) return Result.fail(404, "课程不存在");
        if (course.getStatus() != 0) return Result.fail(400, "请先下架课程再删除");

        // 1. 检查活跃排课
        Long scheduleCount = scheduleMapper.selectCount(
                new LambdaQueryWrapper<ClassSchedule>()
                        .eq(ClassSchedule::getCourseId, courseId)
                        .ne(ClassSchedule::getStatus, "CANCELLED")
        );
        if (scheduleCount != null && scheduleCount > 0) {
            return Result.fail(400, "该课程还有 " + scheduleCount + " 个未取消的排课，请先取消排课");
        }

        // 2. 检查学员剩余课时
        Long balanceCount = balanceMapper.selectCount(
                new LambdaQueryWrapper<StudentBalance>()
                        .eq(StudentBalance::getCourseId, courseId)
                        .gt(StudentBalance::getRemaining, 0)
        );
        if (balanceCount != null && balanceCount > 0) {
            return Result.fail(400, "还有 " + balanceCount + " 名学员有剩余课时未消耗，无法删除");
        }

        // 全部校验通过 → 级联清理
        // 课时包
        packageMapper.delete(new LambdaQueryWrapper<CoursePackage>()
                .eq(CoursePackage::getCourseId, courseId));
        // 已取消排课
        scheduleMapper.delete(new LambdaQueryWrapper<ClassSchedule>()
                .eq(ClassSchedule::getCourseId, courseId));
        // 零课时余额账户
        balanceMapper.delete(new LambdaQueryWrapper<StudentBalance>()
                .eq(StudentBalance::getCourseId, courseId));
        // 课程
        courseMapper.deleteById(courseId);

        return Result.ok("课程及关联数据已全部删除");
    }
}
