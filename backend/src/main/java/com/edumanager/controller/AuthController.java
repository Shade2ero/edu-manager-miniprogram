package com.edumanager.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.edumanager.common.Result;
import com.edumanager.dto.LoginResult;
import com.edumanager.entity.Student;
import com.edumanager.entity.Teacher;
import com.edumanager.mapper.StudentMapper;
import com.edumanager.mapper.TeacherMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 登录与角色 Controller
 *
 * <p>通过微信 OpenID 识别用户身份（学生/教师/管理员），返回角色和权限信息。</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final StudentMapper studentMapper;
    private final TeacherMapper teacherMapper;

    /**
     * 登录（通过 OpenID 识别身份）
     *
     * <pre>
     * POST /api/auth/login
     * Body: { "openid": "dev_openid_001" }
     * </pre>
     *
     * <p>先查教师表，再查学员表，都查不到则自动创建学员账号。</p>
     */
    @PostMapping("/login")
    public Result<LoginResult> login(@RequestBody java.util.Map<String, String> body) {
        String openid = body.get("openid");
        if (openid == null || openid.isEmpty()) {
            return Result.fail("openid 不能为空");
        }

        // 1. 先查教师/管理员
        Teacher teacher = teacherMapper.selectOne(
                new LambdaQueryWrapper<Teacher>()
                        .eq(Teacher::getOpenid, openid)
                        .eq(Teacher::getStatus, 1)
        );
        if (teacher != null) {
            log.info("教师登录: {} ({}), role={}", teacher.getRealName(), openid, teacher.getRole());
            String role = teacher.getRole();
            // ADMIN 可以看到全部 Tab，TEACHER 可以看到签到+课时+排课
            String[] tabs = "ADMIN".equals(role)
                    ? new String[]{"courses", "balance", "schedule", "dashboard"}
                    : new String[]{"balance", "schedule"};
            return Result.ok(LoginResult.builder()
                    .userId(teacher.getId())
                    .role(role)
                    .realName(teacher.getRealName())
                    .institutionId(teacher.getInstitutionId())
                    .token("t_" + openid)
                    .tabs(tabs)
                    .build());
        }

        // 2. 查学生
        Student student = studentMapper.selectOne(
                new LambdaQueryWrapper<Student>()
                        .eq(Student::getOpenid, openid)
                        .eq(Student::getStatus, 1)
        );
        if (student != null) {
            log.info("学员登录: {} ({}), role={}", student.getRealName(), openid, student.getRole());
            // 学生只看签到+购课+课时
            return Result.ok(LoginResult.builder()
                    .userId(student.getId())
                    .role(student.getRole() != null ? student.getRole() : "STUDENT")
                    .realName(student.getRealName())
                    .institutionId(student.getInstitutionId())
                    .token("s_" + openid)
                    .tabs(new String[]{"checkin", "courses", "balance"})
                    .build());
        }

        // 3. 都没查到 → 自动创建学生（实际项目应要求先注册）
        log.info("新用户注册: openid={}", openid);
        Student newStudent = new Student();
        newStudent.setInstitutionId(1L);
        newStudent.setOpenid(openid);
        newStudent.setNickname("新用户");
        newStudent.setRole("STUDENT");
        studentMapper.insert(newStudent);

        return Result.ok(LoginResult.builder()
                .userId(newStudent.getId())
                .role("STUDENT")
                .realName("新用户")
                .institutionId(1L)
                .token("s_" + openid)
                .tabs(new String[]{"checkin", "courses", "balance"})
                .build());
    }
}
