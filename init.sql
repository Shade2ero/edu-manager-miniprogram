-- =======================================
-- 智能课消教务管理系统 — 数据库初始化
-- Docker compose 启动时自动执行
-- =======================================

-- 机构
CREATE TABLE IF NOT EXISTS institution (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    logo_url VARCHAR(500),
    contact_phone VARCHAR(20),
    address VARCHAR(300),
    lbs_latitude DECIMAL(10,7),
    lbs_longitude DECIMAL(10,7),
    lbs_radius INT DEFAULT 300,
    status TINYINT DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO institution (id, name, address, lbs_latitude, lbs_longitude, lbs_radius) VALUES
(1, '友谊路培训中心', '成都市龙泉驿区十陵街道友谊路70号', 30.6484, 104.1837, 3000);

-- 学员
CREATE TABLE IF NOT EXISTS student (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    institution_id BIGINT NOT NULL DEFAULT 1,
    openid VARCHAR(64) NOT NULL,
    unionid VARCHAR(64),
    real_name VARCHAR(50),
    gender VARCHAR(10) DEFAULT '',
    nickname VARCHAR(100),
    avatar_url VARCHAR(500),
    phone VARCHAR(20),
    parent_phone VARCHAR(20),
    role VARCHAR(20) DEFAULT 'STUDENT',
    status TINYINT DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_institution_openid (institution_id, openid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO student (id, institution_id, openid, real_name, gender, phone, role) VALUES
(1, 1, 'dev_openid_001', '张三', '男', '13900001111', 'STUDENT');

-- 教师
CREATE TABLE IF NOT EXISTS teacher (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    institution_id BIGINT NOT NULL DEFAULT 1,
    openid VARCHAR(64) NOT NULL,
    real_name VARCHAR(50) NOT NULL,
    phone VARCHAR(20),
    role VARCHAR(20) NOT NULL DEFAULT 'TEACHER',
    status TINYINT DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_teacher_openid (institution_id, openid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO teacher (id, institution_id, openid, real_name, role) VALUES
(1, 1, 'dev_teacher_001', '李老师', 'TEACHER'),
(2, 1, 'dev_admin_001',  '王管理员', 'ADMIN');

-- 课程
CREATE TABLE IF NOT EXISTS course (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    institution_id BIGINT NOT NULL DEFAULT 1,
    name VARCHAR(100) NOT NULL,
    cover_url VARCHAR(500),
    description TEXT,
    category VARCHAR(50),
    default_duration INT DEFAULT 60,
    status TINYINT DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO course (id, institution_id, name, description, category, default_duration) VALUES
(1, 1, '钢琴启蒙课（初级班）', '适合4-6岁零基础儿童，从识谱到弹奏小曲', '钢琴', 45);

-- 课时包
CREATE TABLE IF NOT EXISTS course_package (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    institution_id BIGINT NOT NULL DEFAULT 1,
    course_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    total_hours INT NOT NULL,
    price INT NOT NULL COMMENT '售价(分)',
    original_price INT COMMENT '原价(分)',
    valid_days INT DEFAULT 365,
    sort_order INT DEFAULT 0,
    status TINYINT DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO course_package (id, institution_id, course_id, name, total_hours, price, original_price, valid_days, sort_order) VALUES
(1, 1, 1, '体验包（4课时）', 4, 79200, 99200, 90, 1),
(2, 1, 1, '春季学期包（16课时）', 16, 298000, 356000, 180, 2),
(3, 1, 1, '全年包（48课时）', 48, 792000, 990000, 365, 3);

-- 课时账户
CREATE TABLE IF NOT EXISTS student_balance (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    institution_id BIGINT NOT NULL DEFAULT 1,
    student_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    total_purchased DECIMAL(8,1) NOT NULL DEFAULT 0,
    total_consumed DECIMAL(8,1) NOT NULL DEFAULT 0,
    remaining DECIMAL(8,1) NOT NULL DEFAULT 0,
    expires_at DATE,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_student_course (institution_id, student_id, course_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO student_balance (id, institution_id, student_id, course_id, total_purchased, total_consumed, remaining, expires_at) VALUES
(1, 1, 1, 1, 0.0, 0.0, 0.0, DATE_ADD(CURDATE(), INTERVAL 180 DAY));

-- 课时流水
CREATE TABLE IF NOT EXISTS balance_transaction (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    institution_id BIGINT NOT NULL DEFAULT 1,
    student_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    balance_id BIGINT NOT NULL,
    change_type VARCHAR(20) NOT NULL COMMENT 'PURCHASE/CONSUME/REFUND/ADJUST',
    change_amount DECIMAL(8,1) NOT NULL,
    balance_before DECIMAL(8,1) NOT NULL,
    balance_after DECIMAL(8,1) NOT NULL,
    biz_type VARCHAR(30),
    biz_id BIGINT,
    remark VARCHAR(200),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_student_course (student_id, course_id),
    INDEX idx_biz (biz_type, biz_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 订单
CREATE TABLE IF NOT EXISTS course_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(32) NOT NULL,
    institution_id BIGINT NOT NULL DEFAULT 1,
    student_id BIGINT NOT NULL,
    package_id BIGINT NOT NULL,
    course_id BIGINT NOT NULL,
    total_amount INT NOT NULL,
    paid_amount INT,
    status VARCHAR(20) DEFAULT 'PENDING',
    pay_time DATETIME,
    wx_transaction_id VARCHAR(64),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_order_no (order_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 排课
CREATE TABLE IF NOT EXISTS class_schedule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    institution_id BIGINT NOT NULL DEFAULT 1,
    course_id BIGINT NOT NULL,
    teacher_id BIGINT NOT NULL,
    classroom VARCHAR(100),
    schedule_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    max_students INT DEFAULT 20,
    status VARCHAR(20) DEFAULT 'SCHEDULED',
    qr_code_token VARCHAR(64),
    qr_code_expire_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_qr_token (qr_code_token),
    INDEX idx_date (schedule_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 点名册
CREATE TABLE IF NOT EXISTS class_student (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    schedule_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    check_in_time DATETIME,
    check_in_method VARCHAR(20),
    check_in_lat DECIMAL(10,7),
    check_in_lng DECIMAL(10,7),
    check_in_device VARCHAR(200),
    risk_flag TINYINT DEFAULT 0,
    risk_reason VARCHAR(200),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_schedule_student (schedule_id, student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 签到日志
CREATE TABLE IF NOT EXISTS attendance_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    institution_id BIGINT NOT NULL DEFAULT 1,
    schedule_id BIGINT NOT NULL,
    class_student_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    check_in_method VARCHAR(20) NOT NULL,
    check_in_time DATETIME NOT NULL,
    latitude DECIMAL(10,7),
    longitude DECIMAL(10,7),
    distance_from_center DECIMAL(10,2),
    device_info VARCHAR(500),
    wx_location_verify TINYINT DEFAULT 0,
    ip_address VARCHAR(45),
    risk_score INT DEFAULT 0,
    risk_detail VARCHAR(500),
    consume_result TINYINT DEFAULT 0,
    consume_transaction_id BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_schedule (schedule_id),
    INDEX idx_student (student_id),
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
