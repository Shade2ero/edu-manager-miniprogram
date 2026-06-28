package com.edumanager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录返回结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResult {

    /** 用户ID（学生或教师的ID） */
    private Long userId;

    /** 角色：STUDENT / TEACHER / ADMIN */
    private String role;

    /** 真实姓名 */
    private String realName;

    /** 机构ID */
    private Long institutionId;

    /** 模拟Token（生产环境换JWT） */
    private String token;

    /** 可访问的页面列表 */
    private String[] tabs;
}
