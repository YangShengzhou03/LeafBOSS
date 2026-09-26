-- 数据库初始化脚本
DROP DATABASE IF EXISTS leaf_boss;
CREATE DATABASE leaf_boss CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE leaf_boss;

-- 管理员表：存储后台管理人员信息
CREATE TABLE admins (
    id CHAR(36) PRIMARY KEY COMMENT '管理员唯一标识符 (UUID)',
    username VARCHAR(50) NOT NULL DEFAULT 'leafAdmin' COMMENT '用户名',
    email VARCHAR(100) UNIQUE NOT NULL COMMENT '邮箱',
    password VARCHAR(255) NOT NULL COMMENT '密码',
    status VARCHAR(20) DEFAULT 'active' NOT NULL COMMENT '状态',
    session_token VARCHAR(64) DEFAULT NULL COMMENT '当前会话Token（单设备登录）',
    register_ip VARCHAR(64) COMMENT '注册IP',
    register_region VARCHAR(64) COMMENT '注册IP归属地',
    last_login_time DATETIME COMMENT '最后登录时间',
    last_login_ip VARCHAR(64) COMMENT '最后登录IP',
    last_login_region VARCHAR(64) COMMENT '最后登录IP归属地',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL COMMENT '最后更新时间',
    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_status (status)
) ENGINE=InnoDB COMMENT='管理员表';

-- 商品表：定义业务商品线
CREATE TABLE products (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '商品唯一标识符',
    name VARCHAR(100) NOT NULL COMMENT '商品名称',
    description TEXT COMMENT '商品描述',
    status VARCHAR(20) DEFAULT 'active' NOT NULL COMMENT '商品状态',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL COMMENT '最后更新时间',
    INDEX idx_name (name),
    INDEX idx_status (status)
) ENGINE=InnoDB COMMENT='商品表';

-- 规格表：定义商品的具体授权规格（如月卡、年卡）
CREATE TABLE specifications (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '规格唯一标识符',
    product_id INT NOT NULL COMMENT '所属商品ID',
    name VARCHAR(100) NOT NULL COMMENT '规格名称',
    price DECIMAL(10,2) DEFAULT 0.00 COMMENT '价格',
    stock_quantity INT DEFAULT 0 COMMENT '当前库存数量',
    valid_days INT DEFAULT 30 COMMENT '卡密有效天数',
    status VARCHAR(20) DEFAULT 'active' NOT NULL COMMENT '状态',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL COMMENT '最后更新时间',
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    INDEX idx_product_id (product_id),
    INDEX idx_name (name),
    INDEX idx_status (status)
) ENGINE=InnoDB COMMENT='规格表';

-- 卡密表：存储生成的卡密
CREATE TABLE card_keys (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '卡密主键ID',
    card_key VARCHAR(100) UNIQUE NOT NULL COMMENT '卡密代码',
    specification_id INT NOT NULL COMMENT '所属规格ID',
    status VARCHAR(20) DEFAULT '未使用' NOT NULL COMMENT '卡密当前状态',
    user_email VARCHAR(100) COMMENT '激活用户的邮箱',
    agent_id CHAR(36) COMMENT '生成该卡密的代理商ID',
    activate_time DATETIME COMMENT '卡密激活时间',
    expire_time DATETIME COMMENT '卡密过期时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL COMMENT '最后更新时间',
    FOREIGN KEY (specification_id) REFERENCES specifications(id) ON DELETE CASCADE,
    INDEX idx_card_key (card_key),
    INDEX idx_status (status),
    INDEX idx_specification_id (specification_id),
    INDEX idx_user_email (user_email(20)),
    INDEX idx_agent_id (agent_id),
    INDEX idx_activate_time (activate_time)
) ENGINE=InnoDB COMMENT='卡密表';

-- 操作日志表：记录系统关键操作
CREATE TABLE operation_logs (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
    operation_type VARCHAR(30) NOT NULL COMMENT '操作类型',
    description TEXT COMMENT '详细描述',
    ip_address VARCHAR(50) COMMENT '操作者IP地址',
    ip_region VARCHAR(100) COMMENT 'IP归属地（省份城市）',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '记录时间',
    INDEX idx_operation_type (operation_type),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB COMMENT='操作日志表';

-- 用户表：统一存储普通用户和代理商（通过 role 区分）
CREATE TABLE users (
    id CHAR(36) PRIMARY KEY COMMENT '用户唯一标识符 (UUID)',
    username VARCHAR(50) UNIQUE NOT NULL COMMENT '用户名',
    email VARCHAR(100) UNIQUE COMMENT '邮箱',
    password VARCHAR(255) COMMENT '密码',
    role ENUM('user', 'agent') NOT NULL DEFAULT 'user' COMMENT '用户类型：user普通用户 agent代理商',
    status VARCHAR(20) DEFAULT 'active' NOT NULL COMMENT '账号状态',
    session_token VARCHAR(64) DEFAULT NULL COMMENT '当前会话Token（单设备登录）',
    registered_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '注册时间',
    register_ip VARCHAR(64) COMMENT '注册IP',
    register_region VARCHAR(64) COMMENT '注册IP归属地',
    last_login_time DATETIME COMMENT '最后登录时间',
    last_login_ip VARCHAR(64) COMMENT '最后登录IP',
    last_login_region VARCHAR(64) COMMENT '最后登录IP归属地',
    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_role (role),
    INDEX idx_status (status)
) ENGINE=InnoDB COMMENT='用户表';

-- 普通用户：已购商品授权记录
CREATE TABLE user_products (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '授权ID',
    user_id CHAR(36) NOT NULL COMMENT '用户ID',
    product_id INT NOT NULL COMMENT '商品ID',
    spec_id INT COMMENT '规格ID（如月卡、年卡）',
    card_key VARCHAR(100) COMMENT '绑定的激活卡密',
    activated_at DATETIME NOT NULL COMMENT '授权激活时间',
    expires_at DATETIME NOT NULL COMMENT '授权到期时间',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1有效 0失效',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    FOREIGN KEY (spec_id) REFERENCES specifications(id) ON DELETE SET NULL,
    INDEX idx_user_id (user_id),
    INDEX idx_product_id (product_id),
    INDEX idx_expires_at (expires_at),
    INDEX idx_status (status)
) ENGINE=InnoDB COMMENT='用户已购商品授权表';

-- 代理商：授权商品及卡密生成额度
CREATE TABLE agent_authorizations (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '授权ID',
    agent_id CHAR(36) NOT NULL COMMENT '代理商用户ID',
    product_id INT NOT NULL COMMENT '授权可销售的商品ID',
    remaining_count INT NOT NULL DEFAULT 0 COMMENT '剩余可生成卡密数（余额，生成即扣减）',
    granted_at DATETIME NOT NULL COMMENT '授权开通时间',
    expires_at DATETIME COMMENT '授权到期时间（NULL为永久）',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1有效 0失效',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL,
    FOREIGN KEY (agent_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    INDEX idx_agent_id (agent_id),
    INDEX idx_product_id (product_id),
    INDEX idx_status (status)
) ENGINE=InnoDB COMMENT='代理商授权表';

-- 公司表：业务关联的公司实体
CREATE TABLE companies (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '公司ID',
    name VARCHAR(100) NOT NULL COMMENT '公司名称',
    comment_count INT DEFAULT 0 COMMENT '关联的评论总数',
    view_count INT DEFAULT 0 COMMENT '浏览量',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL COMMENT '最后更新时间',
    UNIQUE INDEX uk_companies_name (name)
) ENGINE=InnoDB COMMENT='公司表';

-- Boss评论表：用户持卡密对公司的评价
CREATE TABLE boss_reviews (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '评论唯一标识符',
    card_key VARCHAR(100) NOT NULL COMMENT '发表评论时验证的卡密',
    company_id INT NOT NULL COMMENT '所属公司ID',
    content TEXT NOT NULL COMMENT '评论内容',
    like_count INT DEFAULT 0 NOT NULL COMMENT '点赞数',
    dislike_count INT DEFAULT 0 NOT NULL COMMENT '点踩数',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '评论发表时间',
    FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE,
    INDEX idx_card_key (card_key),
    INDEX idx_company_created (company_id, created_at),
    INDEX idx_created_at (created_at),
    FULLTEXT INDEX idx_ft_card_content (card_key, content) WITH PARSER ngram
) ENGINE=InnoDB COMMENT='Boss评论表';

-- 评论投票表：按卡密去重的点赞/点踩记录（一卡一票）
CREATE TABLE review_votes (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '投票ID',
    review_id INT NOT NULL COMMENT '评论ID',
    card_key VARCHAR(100) NOT NULL COMMENT '投票卡密',
    vote VARCHAR(10) NOT NULL COMMENT '投票类型: like / dislike',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL COMMENT '投票时间',
    FOREIGN KEY (review_id) REFERENCES boss_reviews(id) ON DELETE CASCADE,
    UNIQUE INDEX uk_review_card (review_id, card_key)
) ENGINE=InnoDB COMMENT='评论投票表';

-- 通知表
CREATE TABLE IF NOT EXISTS notices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_name VARCHAR(100) NOT NULL COMMENT '商品名称',
    content TEXT NOT NULL COMMENT '通知内容',
    level VARCHAR(20) DEFAULT 'info' COMMENT '通知级别: primary/success/info/warning/error',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_product_name (product_name)
) ENGINE=InnoDB COMMENT='商品通知表';

-- 商品反馈表
CREATE TABLE IF NOT EXISTS feedback (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_name VARCHAR(50) NOT NULL COMMENT '用户名',
    content TEXT NOT NULL COMMENT '反馈内容',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB COMMENT='商品反馈表';

-- 初始化默认管理员账号 (密码: 123456)
INSERT INTO admins (id, username, email, password, status) VALUES
('550e8400-e29b-41d4-a716-446655440000', 'admin', 'admin@qq.com', '123456', 'active');

COMMIT;
