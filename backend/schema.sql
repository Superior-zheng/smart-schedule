-- Smart Schedule - minimal schema (MySQL)
-- Note: MyBatis-Plus默认使用驼峰转下划线，因此 durationMinutes 对应 duration_minutes

CREATE TABLE IF NOT EXISTS task (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(255) NOT NULL,
  content TEXT,
  priority INT,
  status INT DEFAULT 0,
  owner VARCHAR(50) NOT NULL,
  deadline DATETIME NULL,
  duration_minutes INT NULL,
  task_type INT DEFAULT 1 COMMENT '1-提醒型,2-学习型',
  create_time DATETIME NULL
);

-- 旧库升级可执行（若已存在该列可忽略报错）
-- ALTER TABLE task ADD COLUMN task_type INT DEFAULT 1 COMMENT '1-提醒型,2-学习型';

-- 登录用户表（修正后的建表语句）
CREATE TABLE IF NOT EXISTS `user` (
  `username` VARCHAR(50) NOT NULL COMMENT '账号',
  `password` VARCHAR(100) NOT NULL COMMENT '密码',
  `role` TINYINT DEFAULT 0 COMMENT '角色：0-普通用户, 1-管理员',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 用户昵称字段（升级脚本）
ALTER TABLE `user` ADD COLUMN IF NOT EXISTS `nickname` VARCHAR(50) NULL COMMENT '昵称';

-- 打卡项目表
CREATE TABLE IF NOT EXISTS `habit` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `name` VARCHAR(100) NOT NULL COMMENT '打卡项名称',
  `color` VARCHAR(20) DEFAULT '#409EFF' COMMENT '颜色',
  `owner` VARCHAR(50) NOT NULL COMMENT '归属用户',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_owner` (`owner`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 打卡记录表
CREATE TABLE IF NOT EXISTS `checkin_record` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `habit_id` BIGINT NOT NULL COMMENT '打卡项ID',
  `owner` VARCHAR(50) NOT NULL COMMENT '归属用户',
  `checkin_date` DATE NOT NULL COMMENT '打卡日期',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_habit_date` (`habit_id`, `checkin_date`),
  INDEX `idx_owner` (`owner`),
  INDEX `idx_date` (`checkin_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 愿景板/倒数日表
CREATE TABLE IF NOT EXISTS `vision_item` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `name` VARCHAR(200) NOT NULL COMMENT '名称',
  `target_date` DATE NOT NULL COMMENT '目标日期',
  `mode` VARCHAR(20) DEFAULT 'countdown' COMMENT 'countdown-倒计时,countup-正计时',
  `repeat_mode` VARCHAR(20) DEFAULT 'none' COMMENT 'none/weekly/monthly/yearly',
  `owner` VARCHAR(50) NOT NULL COMMENT '归属用户',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_owner` (`owner`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 学习资料库表
CREATE TABLE IF NOT EXISTS `library_link` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `url` VARCHAR(500) NOT NULL COMMENT '网址',
  `note` VARCHAR(500) COMMENT '备注',
  `owner` VARCHAR(50) NOT NULL COMMENT '归属用户',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_owner` (`owner`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
