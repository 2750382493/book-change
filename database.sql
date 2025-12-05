-- campus_book_complete_mysql.sql
-- 完整的 MySQL 兼容版本，适用于 XAMPP

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 创建数据库
DROP DATABASE IF EXISTS campus_book;
CREATE DATABASE IF NOT EXISTS campus_book 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE campus_book;

-- ======================== 1. 用户表 (users) ========================
CREATE TABLE IF NOT EXISTS users (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id CHAR(36) NOT NULL UNIQUE COMMENT 'UUID, 用于API',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password_hash VARCHAR(255) NOT NULL COMMENT '密码哈希',
    email VARCHAR(100) UNIQUE COMMENT '邮箱',
    phone VARCHAR(20) COMMENT '手机号',
    nickname VARCHAR(50) COMMENT '昵称',
    avatar_url VARCHAR(500) COMMENT '头像URL',
    bio TEXT COMMENT '个人简介',
    
    -- 状态与角色
    status TINYINT UNSIGNED DEFAULT 0 COMMENT '0:正常, 1:禁用, 2:未激活',
    is_admin TINYINT(1) DEFAULT 0 COMMENT '是否管理员',
    email_verified TINYINT(1) DEFAULT 0 COMMENT '邮箱是否验证',
    
    -- 统计信息
    book_count INT UNSIGNED DEFAULT 0 COMMENT '发布的书籍数量',
    need_count INT UNSIGNED DEFAULT 0 COMMENT '发布的需求数量',
    sold_count INT UNSIGNED DEFAULT 0 COMMENT '已售书籍数量',
    rating DECIMAL(3,2) DEFAULT 5.00 COMMENT '用户评分',
    rating_count INT UNSIGNED DEFAULT 0 COMMENT '评分次数',
    
    -- 隐私设置
    show_phone TINYINT(1) DEFAULT 0 COMMENT '是否公开手机号',
    show_email TINYINT(1) DEFAULT 0 COMMENT '是否公开邮箱',
    
    -- 社交信息
    wechat VARCHAR(50) COMMENT '微信',
    qq VARCHAR(20) COMMENT 'QQ',
    
    -- 时间戳
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP NULL DEFAULT NULL,
    
    -- 索引
    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_phone (phone),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    INDEX idx_rating (rating)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ======================== 2. 用户角色表 (user_roles) ========================
CREATE TABLE IF NOT EXISTS user_roles (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id INT UNSIGNED NOT NULL,
    role VARCHAR(50) NOT NULL COMMENT '角色名称: ROLE_USER, ROLE_ADMIN, ROLE_VIP',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_role (user_id, role),
    INDEX idx_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色表';

-- ======================== 3. 书籍表 (books) ========================
CREATE TABLE IF NOT EXISTS books (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    book_id CHAR(36) NOT NULL UNIQUE COMMENT 'UUID, 用于API',
    title VARCHAR(200) NOT NULL COMMENT '书名',
    author VARCHAR(100) COMMENT '作者',
    publisher VARCHAR(100) COMMENT '出版社',
    isbn VARCHAR(20) COMMENT 'ISBN',
    description TEXT COMMENT '描述',
    
    -- 价格信息
    price DECIMAL(10,2) NOT NULL COMMENT '价格',
    original_price DECIMAL(10,2) COMMENT '原价',
    negotiable TINYINT(1) DEFAULT 1 COMMENT '是否可议价',
    
    -- 分类与状态
    category VARCHAR(50) NOT NULL COMMENT '分类',
    book_condition VARCHAR(20) DEFAULT '良好' COMMENT '新旧程度',
    
    -- 关联信息
    seller_id INT UNSIGNED NOT NULL COMMENT '卖家ID',
    
    -- 状态
    sold TINYINT(1) DEFAULT 0 COMMENT '是否已售',
    active TINYINT(1) DEFAULT 1 COMMENT '是否上架',
    
    -- 图片（使用JSON存储URL数组）
    images JSON COMMENT '图片URL数组',
    
    -- 扩展信息
    condition_detail TEXT COMMENT '新旧程度详细描述',
    location VARCHAR(100) COMMENT '位置',
    delivery_method VARCHAR(20) DEFAULT '面交' COMMENT '交付方式',
    
    -- 时间戳
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    sold_at TIMESTAMP NULL DEFAULT NULL,
    
    -- 外键约束
    FOREIGN KEY (seller_id) REFERENCES users(id) ON DELETE CASCADE,
    
    -- 索引
    INDEX idx_title (title(100)),
    INDEX idx_author (author),
    INDEX idx_isbn (isbn),
    INDEX idx_category (category),
    INDEX idx_price (price),
    INDEX idx_seller (seller_id),
    INDEX idx_status (sold, active),
    INDEX idx_created_at (created_at),
    FULLTEXT idx_search (title, author, description)  -- 全文搜索
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='书籍表';

-- ======================== 4. 书籍标签表 (book_tags) ========================
CREATE TABLE IF NOT EXISTS book_tags (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    book_id INT UNSIGNED NOT NULL,
    tag VARCHAR(50) NOT NULL,
    
    FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE,
    UNIQUE KEY uk_book_tag (book_id, tag),
    INDEX idx_tag (tag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='书籍标签表';

-- ======================== 5. 需求表 (needs) ========================
CREATE TABLE IF NOT EXISTS needs (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    need_id CHAR(36) NOT NULL UNIQUE COMMENT 'UUID, 用于API',
    title VARCHAR(200) NOT NULL COMMENT '标题',
    author VARCHAR(100) COMMENT '作者',
    isbn VARCHAR(20) COMMENT 'ISBN',
    description TEXT COMMENT '描述',
    
    -- 价格范围
    max_price DECIMAL(10,2) COMMENT '最高价格',
    min_price DECIMAL(10,2) COMMENT '最低价格',
    negotiable TINYINT(1) DEFAULT 1 COMMENT '是否可议价',
    
    -- 分类与要求
    category VARCHAR(50) NOT NULL COMMENT '分类',
    condition_requirement VARCHAR(50) DEFAULT '良好及以上' COMMENT '新旧要求',
    require_original TINYINT(1) DEFAULT 0 COMMENT '是否要求正版',
    require_invoice TINYINT(1) DEFAULT 0 COMMENT '是否要求发票',
    require_delivery TINYINT(1) DEFAULT 0 COMMENT '是否要求配送',
    
    -- 联系信息
    contact_method VARCHAR(20) DEFAULT '站内信' COMMENT '联系方式',
    contact_info VARCHAR(200) COMMENT '具体联系方式',
    
    -- 关联信息
    user_id INT UNSIGNED NOT NULL COMMENT '用户ID',
    
    -- 状态
    status ENUM('DRAFT', 'ACTIVE', 'MATCHED', 'NEGOTIATING', 'PENDING', 'FULFILLED', 'EXPIRED', 'CANCELLED', 'ARCHIVED') DEFAULT 'ACTIVE',
    fulfilled TINYINT(1) DEFAULT 0 COMMENT '是否已满足',
    
    -- 匹配信息
    matched_book_id INT UNSIGNED COMMENT '匹配的书籍ID',
    match_score INT UNSIGNED COMMENT '匹配度评分(0-100)',
    
    -- 优先级
    urgency TINYINT UNSIGNED DEFAULT 5 COMMENT '紧急程度 1-10',
    priority ENUM('LOW', 'NORMAL', 'HIGH', 'URGENT') DEFAULT 'NORMAL' COMMENT '优先级',
    
    -- 统计
    view_count INT UNSIGNED DEFAULT 0 COMMENT '浏览次数',
    favorite_count INT UNSIGNED DEFAULT 0 COMMENT '收藏次数',
    offer_count INT UNSIGNED DEFAULT 0 COMMENT '报价次数',
    
    -- 时间管理
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    expiry_date DATE COMMENT '过期日期',
    fulfill_at TIMESTAMP NULL DEFAULT NULL COMMENT '满足时间',
    match_at TIMESTAMP NULL DEFAULT NULL COMMENT '匹配时间',
    
    -- 外键约束
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (matched_book_id) REFERENCES books(id) ON DELETE SET NULL,
    
    -- 索引
    INDEX idx_title (title(100)),
    INDEX idx_isbn (isbn),
    INDEX idx_category (category),
    INDEX idx_max_price (max_price),
    INDEX idx_user (user_id),
    INDEX idx_status (status),
    INDEX idx_urgency (urgency),
    INDEX idx_priority (priority),
    INDEX idx_expiry (expiry_date),
    INDEX idx_created_at (created_at),
    FULLTEXT idx_need_search (title, author, description)  -- 全文搜索
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='需求表';

-- ======================== 6. 需求标签表 (need_tags) ========================
CREATE TABLE IF NOT EXISTS need_tags (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    need_id INT UNSIGNED NOT NULL,
    tag VARCHAR(50) NOT NULL,
    
    FOREIGN KEY (need_id) REFERENCES needs(id) ON DELETE CASCADE,
    UNIQUE KEY uk_need_tag (need_id, tag),
    INDEX idx_tag (tag)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='需求标签表';

-- ======================== 7. 消息表 (messages) ========================
CREATE TABLE IF NOT EXISTS messages (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    message_id CHAR(36) NOT NULL UNIQUE COMMENT 'UUID',
    thread_id VARCHAR(100) NOT NULL COMMENT '对话线程ID',
    
    -- 参与者
    from_user_id INT UNSIGNED NOT NULL COMMENT '发送者ID',
    to_user_id INT UNSIGNED NOT NULL COMMENT '接收者ID',
    book_id INT UNSIGNED COMMENT '关联的书籍ID',
    
    -- 消息内容
    content TEXT NOT NULL COMMENT '消息内容',
    message_type ENUM('TEXT', 'IMAGE', 'FILE', 'VOICE', 'LOCATION', 'SYSTEM', 'TRANSACTION', 'REMINDER', 'CUSTOM') DEFAULT 'TEXT',
    
    -- 富文本内容
    rich_content JSON COMMENT '富文本内容(JSON)',
    
    -- 状态
    status ENUM('SENDING', 'SENT', 'DELIVERED', 'READ', 'WITHDRAWN', 'FAILED', 'EXPIRED') DEFAULT 'SENT',
    is_read TINYINT(1) DEFAULT 0 COMMENT '是否已读',
    is_withdrawn TINYINT(1) DEFAULT 0 COMMENT '是否已撤回',
    
    -- 删除状态
    deleted_by_sender TINYINT(1) DEFAULT 0 COMMENT '发送者是否删除',
    deleted_by_receiver TINYINT(1) DEFAULT 0 COMMENT '接收者是否删除',
    
    -- 时间戳
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP NULL DEFAULT NULL COMMENT '阅读时间',
    delivered_at TIMESTAMP NULL DEFAULT NULL COMMENT '送达时间',
    withdraw_at TIMESTAMP NULL DEFAULT NULL COMMENT '撤回时间',
    delete_at TIMESTAMP NULL DEFAULT NULL COMMENT '删除时间',
    
    -- 回复关系
    reply_to_id INT UNSIGNED COMMENT '回复的消息ID',
    quote_text TEXT COMMENT '引用文本',
    
    -- 元数据
    metadata JSON COMMENT '元数据(JSON)',
    
    -- 外键约束
    FOREIGN KEY (from_user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (to_user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE SET NULL,
    FOREIGN KEY (reply_to_id) REFERENCES messages(id) ON DELETE SET NULL,
    
    -- 索引
    INDEX idx_thread (thread_id, created_at),
    INDEX idx_from_user (from_user_id, created_at),
    INDEX idx_to_user (to_user_id, created_at),
    INDEX idx_book (book_id),
    INDEX idx_status (status),
    INDEX idx_is_read (is_read),
    INDEX idx_created_at (created_at),
    INDEX idx_reply (reply_to_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息表';

-- ======================== 8. 消息附件表 (message_attachments) ========================
CREATE TABLE IF NOT EXISTS message_attachments (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    message_id INT UNSIGNED NOT NULL COMMENT '消息ID',
    file_name VARCHAR(255) NOT NULL COMMENT '文件名',
    file_url VARCHAR(500) NOT NULL COMMENT '文件URL',
    file_type VARCHAR(50) COMMENT '文件类型: image, pdf, word等',
    file_size BIGINT UNSIGNED COMMENT '文件大小(字节)',
    width INT UNSIGNED COMMENT '图片宽度',
    height INT UNSIGNED COMMENT '图片高度',
    duration INT UNSIGNED COMMENT '音频/视频时长(秒)',
    thumbnail_url VARCHAR(500) COMMENT '缩略图URL',
    
    FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE CASCADE,
    INDEX idx_message (message_id),
    INDEX idx_file_type (file_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息附件表';

-- ======================== 9. 交易记录表 (transactions) ========================
CREATE TABLE IF NOT EXISTS transactions (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    transaction_id CHAR(36) NOT NULL UNIQUE COMMENT 'UUID',
    book_id INT UNSIGNED NOT NULL COMMENT '书籍ID',
    buyer_id INT UNSIGNED NOT NULL COMMENT '买家ID',
    seller_id INT UNSIGNED NOT NULL COMMENT '卖家ID',
    
    -- 交易信息
    transaction_price DECIMAL(10,2) NOT NULL COMMENT '交易价格',
    transaction_status ENUM('PENDING', 'PAID', 'DELIVERING', 'COMPLETED', 'CANCELLED', 'REFUNDED') DEFAULT 'PENDING',
    payment_method VARCHAR(50) COMMENT '支付方式',
    payment_id VARCHAR(100) COMMENT '第三方支付ID',
    
    -- 物流信息
    delivery_method VARCHAR(50) COMMENT '配送方式',
    tracking_number VARCHAR(100) COMMENT '快递单号',
    delivery_address TEXT COMMENT '收货地址',
    delivery_contact VARCHAR(50) COMMENT '收货人',
    delivery_phone VARCHAR(20) COMMENT '收货电话',
    
    -- 评价
    buyer_rating TINYINT UNSIGNED COMMENT '买家给卖家的评分 1-5',
    buyer_comment TEXT COMMENT '买家评价',
    seller_rating TINYINT UNSIGNED COMMENT '卖家给买家的评分 1-5',
    seller_comment TEXT COMMENT '卖家评价',
    
    -- 时间戳
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    paid_at TIMESTAMP NULL DEFAULT NULL COMMENT '支付时间',
    delivered_at TIMESTAMP NULL DEFAULT NULL COMMENT '发货时间',
    completed_at TIMESTAMP NULL DEFAULT NULL COMMENT '完成时间',
    cancelled_at TIMESTAMP NULL DEFAULT NULL COMMENT '取消时间',
    refunded_at TIMESTAMP NULL DEFAULT NULL COMMENT '退款时间',
    
    -- 外键约束
    FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE,
    FOREIGN KEY (buyer_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (seller_id) REFERENCES users(id) ON DELETE CASCADE,
    
    -- 索引
    INDEX idx_book (book_id),
    INDEX idx_buyer (buyer_id),
    INDEX idx_seller (seller_id),
    INDEX idx_status (transaction_status),
    INDEX idx_created_at (created_at),
    INDEX idx_payment (payment_method, payment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='交易记录表';

-- ======================== 10. 收藏表 (favorites) ========================
CREATE TABLE IF NOT EXISTS favorites (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id INT UNSIGNED NOT NULL COMMENT '用户ID',
    target_type ENUM('BOOK', 'NEED') NOT NULL COMMENT '收藏类型',
    target_id INT UNSIGNED NOT NULL COMMENT '收藏目标ID',
    notes TEXT COMMENT '备注',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_favorite (user_id, target_type, target_id),
    INDEX idx_user (user_id, target_type),
    INDEX idx_target (target_type, target_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收藏表';

-- ======================== 11. 图片表 (images) ========================
CREATE TABLE IF NOT EXISTS images (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    image_id CHAR(36) NOT NULL UNIQUE COMMENT 'UUID',
    user_id INT UNSIGNED NOT NULL COMMENT '上传用户ID',
    file_name VARCHAR(255) NOT NULL COMMENT '文件名',
    file_path VARCHAR(500) NOT NULL COMMENT '文件路径',
    file_size BIGINT UNSIGNED NOT NULL COMMENT '文件大小(字节)',
    file_type VARCHAR(50) NOT NULL COMMENT '文件类型',
    width INT UNSIGNED COMMENT '宽度',
    height INT UNSIGNED COMMENT '高度',
    thumbnail_path VARCHAR(500) COMMENT '缩略图路径',
    
    -- 关联信息
    entity_type ENUM('BOOK', 'USER', 'MESSAGE') COMMENT '关联实体类型',
    entity_id INT UNSIGNED COMMENT '关联实体ID',
    
    -- 元数据
    description TEXT COMMENT '描述',
    tags TEXT COMMENT '标签(逗号分隔)',
    
    -- 状态
    is_temp TINYINT(1) DEFAULT 1 COMMENT '是否为临时图片',
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP NULL DEFAULT NULL,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user (user_id),
    INDEX idx_entity (entity_type, entity_id),
    INDEX idx_file_type (file_type),
    INDEX idx_is_temp (is_temp, uploaded_at),
    INDEX idx_uploaded_at (uploaded_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='图片表';

-- ======================== 12. 系统配置表 (system_config) ========================
CREATE TABLE IF NOT EXISTS system_config (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    config_key VARCHAR(100) NOT NULL UNIQUE COMMENT '配置键',
    config_value TEXT NOT NULL COMMENT '配置值',
    config_type ENUM('STRING', 'NUMBER', 'BOOLEAN', 'JSON') DEFAULT 'STRING' COMMENT '配置类型',
    description TEXT COMMENT '描述',
    is_public TINYINT(1) DEFAULT 0 COMMENT '是否公开',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_config_key (config_key),
    INDEX idx_is_public (is_public)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';

-- ======================== 13. 消息提及表 (message_mentions) ========================
CREATE TABLE IF NOT EXISTS message_mentions (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    message_id INT UNSIGNED NOT NULL COMMENT '消息ID',
    user_id INT UNSIGNED NOT NULL COMMENT '被提及的用户ID',
    start_index INT UNSIGNED COMMENT '在内容中的起始位置',
    end_index INT UNSIGNED COMMENT '在内容中的结束位置',
    
    FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_message_user (message_id, user_id),
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息提及表';

-- ======================== 14. 浏览历史表 (view_history) ========================
CREATE TABLE IF NOT EXISTS view_history (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id INT UNSIGNED NOT NULL COMMENT '用户ID',
    target_type ENUM('BOOK', 'NEED') NOT NULL COMMENT '浏览类型',
    target_id INT UNSIGNED NOT NULL COMMENT '浏览目标ID',
    view_count INT UNSIGNED DEFAULT 1 COMMENT '浏览次数',
    last_viewed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_view (user_id, target_type, target_id),
    INDEX idx_target (target_type, target_id),
    INDEX idx_last_viewed (last_viewed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='浏览历史表';

-- ======================== 15. 搜索历史表 (search_history) ========================
CREATE TABLE IF NOT EXISTS search_history (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id INT UNSIGNED NOT NULL COMMENT '用户ID',
    keyword VARCHAR(200) NOT NULL COMMENT '搜索关键词',
    search_type ENUM('BOOK', 'NEED', 'ALL') DEFAULT 'ALL' COMMENT '搜索类型',
    result_count INT UNSIGNED COMMENT '结果数量',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user (user_id),
    INDEX idx_keyword (keyword(50)),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='搜索历史表';

-- ======================== 16. 通知表 (notifications) ========================
CREATE TABLE IF NOT EXISTS notifications (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    user_id INT UNSIGNED NOT NULL COMMENT '接收用户ID',
    title VARCHAR(200) NOT NULL COMMENT '通知标题',
    content TEXT NOT NULL COMMENT '通知内容',
    notification_type ENUM('SYSTEM', 'MESSAGE', 'TRANSACTION', 'MATCH', 'OTHER') DEFAULT 'SYSTEM' COMMENT '通知类型',
    is_read TINYINT(1) DEFAULT 0 COMMENT '是否已读',
    related_id INT UNSIGNED COMMENT '关联ID',
    related_type VARCHAR(50) COMMENT '关联类型',
    metadata JSON COMMENT '元数据',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP NULL DEFAULT NULL,
    
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user (user_id, is_read),
    INDEX idx_type (notification_type),
    INDEX idx_created_at (created_at),
    INDEX idx_is_read (is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知表';

-- ======================== 17. 匹配记录表 (matches) ========================
CREATE TABLE IF NOT EXISTS matches (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    need_id INT UNSIGNED NOT NULL COMMENT '需求ID',
    book_id INT UNSIGNED NOT NULL COMMENT '书籍ID',
    match_score INT UNSIGNED NOT NULL COMMENT '匹配度评分(0-100)',
    match_reason TEXT COMMENT '匹配原因',
    is_notified TINYINT(1) DEFAULT 0 COMMENT '是否已通知',
    is_accepted TINYINT(1) DEFAULT 0 COMMENT '是否被接受',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (need_id) REFERENCES needs(id) ON DELETE CASCADE,
    FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE,
    UNIQUE KEY uk_need_book (need_id, book_id),
    INDEX idx_match_score (match_score DESC),
    INDEX idx_is_notified (is_notified),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='匹配记录表';

-- ======================== 18. 举报表 (reports) ========================
CREATE TABLE IF NOT EXISTS reports (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    reporter_id INT UNSIGNED NOT NULL COMMENT '举报人ID',
    target_type ENUM('USER', 'BOOK', 'NEED', 'MESSAGE') NOT NULL COMMENT '举报目标类型',
    target_id INT UNSIGNED NOT NULL COMMENT '举报目标ID',
    reason ENUM('SPAM', 'FRAUD', 'INAPPROPRIATE', 'HARASSMENT', 'OTHER') NOT NULL COMMENT '举报原因',
    description TEXT COMMENT '详细描述',
    evidence TEXT COMMENT '证据(图片URL等)',
    status ENUM('PENDING', 'PROCESSING', 'RESOLVED', 'DISMISSED') DEFAULT 'PENDING' COMMENT '处理状态',
    admin_notes TEXT COMMENT '管理员备注',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP NULL DEFAULT NULL,
    
    FOREIGN KEY (reporter_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_target (target_type, target_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='举报表';

-- ======================== 插入初始数据 ========================

-- 插入系统配置数据
INSERT INTO system_config (config_key, config_value, config_type, description, is_public) VALUES
('app_name', '校园二手书交易平台', 'STRING', '应用名称', 1),
('app_version', '1.0.0', 'STRING', '应用版本', 1),
('jwt_secret', 'campus_book_jwt_secret_2024_change_this_in_production', 'STRING', 'JWT密钥', 0),
('jwt_expiration_hours', '24', 'NUMBER', 'JWT过期时间(小时)', 0),
('max_upload_size_mb', '10', 'NUMBER', '最大上传文件大小(MB)', 1),
('allow_registration', 'true', 'BOOLEAN', '是否允许注册', 1),
('default_user_avatar', '/uploads/default-avatar.png', 'STRING', '默认用户头像', 1),
('site_url', 'http://localhost:8080', 'STRING', '站点URL', 0),
('book_expiry_days', '30', 'NUMBER', '书籍默认过期天数', 0),
('need_expiry_days', '30', 'NUMBER', '需求默认过期天数', 0),
('match_threshold', '60', 'NUMBER', '匹配阈值(0-100)', 0),
('max_images_per_book', '5', 'NUMBER', '每本书最大图片数', 1),
('contact_email', 'admin@campusbook.com', 'STRING', '联系邮箱', 1),
('site_description', '校园二手书交易平台，让知识流动起来', 'STRING', '站点描述', 1),
('enable_email_verification', 'false', 'BOOLEAN', '是否启用邮箱验证', 0),
('enable_sms_verification', 'false', 'BOOLEAN', '是否启用短信验证', 0),
('max_books_per_user', '20', 'NUMBER', '每个用户最多发布书籍数', 1),
('max_needs_per_user', '10', 'NUMBER', '每个用户最多发布需求数', 1),
('transaction_fee_rate', '0', 'NUMBER', '交易手续费率(%)', 1),
('enable_chat', 'true', 'BOOLEAN', '是否启用聊天功能', 1);

-- 插入测试用户 (密码均为: 123456)
INSERT INTO users (user_id, username, password_hash, email, nickname, is_admin, email_verified, status) VALUES 
('00000000-0000-0000-0000-000000000001', 'admin', '$2a$10$YourHashedPasswordHere', 'admin@campusbook.com', '管理员', 1, 1, 0),
('00000000-0000-0000-0000-000000000002', 'testuser', '$2a$10$YourHashedPasswordHere', 'user@campusbook.com', '测试用户', 0, 1, 0),
('00000000-0000-0000-0000-000000000003', 'seller1', '$2a$10$YourHashedPasswordHere', 'seller1@campusbook.com', '卖家1号', 0, 1, 0),
('00000000-0000-0000-0000-000000000004', 'buyer1', '$2a$10$YourHashedPasswordHere', 'buyer1@campusbook.com', '买家1号', 0, 1, 0);

-- 为用户添加角色
INSERT INTO user_roles (user_id, role) VALUES
(1, 'ROLE_ADMIN'),
(1, 'ROLE_USER'),
(2, 'ROLE_USER'),
(2, 'ROLE_VIP'),
(3, 'ROLE_USER'),
(4, 'ROLE_USER');

-- 插入测试书籍
INSERT INTO books (book_id, title, author, publisher, isbn, description, price, original_price, category, book_condition, seller_id, images) VALUES
('10000000-0000-0000-0000-000000000001', 'Java编程思想', 'Bruce Eckel', '机械工业出版社', '9787111213826', '经典的Java编程书籍，适合初学者和进阶者', 45.00, 108.00, '计算机', '九成新', 3, '["/uploads/books/java1.jpg", "/uploads/books/java2.jpg"]'),
('10000000-0000-0000-0000-000000000002', '算法导论', 'Thomas H. Cormen', '机械工业出版社', '9787111407010', '算法领域的经典教材', 60.00, 128.00, '计算机', '八成新', 3, '["/uploads/books/algo1.jpg"]'),
('10000000-0000-0000-0000-000000000003', '百年孤独', '加西亚·马尔克斯', '南海出版公司', '9787544253994', '魔幻现实主义文学的代表作', 25.00, 39.80, '文学', '良好', 2, '["/uploads/books/literature1.jpg"]'),
('10000000-0000-0000-0000-000000000004', '经济学原理', '曼昆', '北京大学出版社', '9787301150894', '经济学入门经典教材', 40.00, 88.00, '经济', '七成新', 2, '["/uploads/books/econ1.jpg"]'),
('10000000-0000-0000-0000-000000000005', '高等数学', '同济大学数学系', '高等教育出版社', '9787040396638', '大学高等数学教材', 20.00, 45.00, '数学', '良好', 3, '["/uploads/books/math1.jpg"]');

-- 为书籍添加标签
INSERT INTO book_tags (book_id, tag) VALUES
(1, '编程'), (1, 'Java'), (1, '计算机'),
(2, '算法'), (2, '计算机'), (2, '数据结构'),
(3, '文学'), (3, '小说'), (3, '魔幻现实主义'),
(4, '经济学'), (4, '教材'), (4, '大学'),
(5, '数学'), (5, '高等数学'), (5, '教材');

-- 插入测试需求
INSERT INTO needs (need_id, title, author, isbn, description, max_price, min_price, category, condition_requirement, user_id, urgency, priority, expiry_date) VALUES
('20000000-0000-0000-0000-000000000001', '求购：C++ Primer', 'Stanley B. Lippman', '9787111553077', '需要C++ Primer第五版，最好是正版', 50.00, 30.00, '计算机', '八成新及以上', 4, 8, 'HIGH', DATE_ADD(CURDATE(), INTERVAL 15 DAY)),
('20000000-0000-0000-0000-000000000002', '求购：数据结构与算法分析', 'Mark Allen Weiss', '9787115390592', '数据结构教材，最好有习题解答', 40.00, NULL, '计算机', '良好及以上', 4, 5, 'NORMAL', DATE_ADD(CURDATE(), INTERVAL 30 DAY)),
('20000000-0000-0000-0000-000000000003', '求购：红楼梦', '曹雪芹', '9787020002207', '人民文学出版社版本，要求无破损', 20.00, 10.00, '文学', '七成新及以上', 2, 3, 'LOW', DATE_ADD(CURDATE(), INTERVAL 60 DAY));

-- 为需求添加标签
INSERT INTO need_tags (need_id, tag) VALUES
(1, 'C++'), (1, '编程'), (1, '计算机'),
(2, '数据结构'), (2, '算法'), (2, '计算机'),
(3, '文学'), (3, '古典文学'), (3, '小说');

-- 插入测试消息
INSERT INTO messages (message_id, thread_id, from_user_id, to_user_id, book_id, content, message_type, status, is_read) VALUES
('30000000-0000-0000-0000-000000000001', 'thread_3_4_1', 4, 3, 1, '你好，请问《Java编程思想》这本书还在吗？', 'TEXT', 'READ', 1),
('30000000-0000-0000-0000-000000000002', 'thread_3_4_1', 3, 4, 1, '还在的，你需要吗？', 'TEXT', 'READ', 1),
('30000000-0000-0000-0000-000000000003', 'thread_3_4_1', 4, 3, 1, '价格还能商量吗？', 'TEXT', 'READ', 1),
('30000000-0000-0000-0000-000000000004', 'thread_3_4_1', 3, 4, 1, '最低40元，不能再少了', 'TEXT', 'DELIVERED', 0);

-- 插入测试交易
INSERT INTO transactions (transaction_id, book_id, buyer_id, seller_id, transaction_price, transaction_status, payment_method) VALUES
('40000000-0000-0000-0000-000000000001', 1, 4, 3, 40.00, 'COMPLETED', '支付宝'),
('40000000-0000-0000-0000-000000000002', 2, 2, 3, 55.00, 'PAID', '微信支付'),
('40000000-0000-0000-0000-000000000003', 3, 4, 2, 25.00, 'PENDING', NULL);

-- 更新相关状态
UPDATE books SET sold = 1, sold_at = NOW() WHERE id IN (1, 2);
UPDATE users SET sold_count = sold_count + 1 WHERE id = 3;
UPDATE users SET sold_count = sold_count + 1 WHERE id = 2;

-- 插入测试收藏
INSERT INTO favorites (user_id, target_type, target_id) VALUES
(2, 'BOOK', 4),
(3, 'BOOK', 5),
(4, 'NEED', 2),
(2, 'NEED', 1);

-- 插入测试通知
INSERT INTO notifications (user_id, title, content, notification_type, is_read) VALUES
(4, '交易完成', '您的购买订单已完成，请对卖家进行评价', 'TRANSACTION', 0),
(3, '新消息', '您有一条新消息', 'MESSAGE', 0),
(2, '系统通知', '欢迎使用校园二手书交易平台', 'SYSTEM', 1);

-- 插入测试匹配记录
INSERT INTO matches (need_id, book_id, match_score, match_reason) VALUES
(1, 1, 85, 'ISBN匹配，价格合理'),
(2, 2, 90, '科目匹配，评分高');

-- 开启外键约束
SET FOREIGN_KEY_CHECKS = 1;

-- 显示表信息
SHOW TABLES;

-- 显示各表记录数
SELECT 
    'users' AS table_name,
    COUNT(*) AS record_count
FROM users
UNION ALL
SELECT 
    'books',
    COUNT(*)
FROM books
UNION ALL
SELECT 
    'needs',
    COUNT(*)
FROM needs
UNION ALL
SELECT 
    'messages',
    COUNT(*)
FROM messages
UNION ALL
SELECT 
    'transactions',
    COUNT(*)
FROM transactions
UNION ALL
SELECT 
    'favorites',
    COUNT(*)
FROM favorites
ORDER BY table_name;