-- =========================================================================
-- 1. BẢNG CẤU HÌNH GÓI DỊCH VỤ ĐỘNG (ADMIN_PACKAGES)
-- =========================================================================
CREATE TABLE IF NOT EXISTS admin_packages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    package_type VARCHAR(50) NOT NULL,
    package_category VARCHAR(50) NOT NULL DEFAULT 'SUBSCRIPTION',
    price BIGINT NOT NULL,
    credit_limit DOUBLE NOT NULL,
    duration INT NOT NULL DEFAULT 30,
    model_package_level INT,
    description VARCHAR(255),
    storage_quota_mb BIGINT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    UNIQUE INDEX idx_admin_packages_type (package_type)
);

-- =========================================================================
-- 1.1 BẢNG CẤU HÌNH CÁC GÓI MODEL AI
-- =========================================================================
CREATE TABLE IF NOT EXISTS ai_model_packages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    credit_rate DOUBLE NOT NULL DEFAULT 1.0,
    models JSON NOT NULL,
    description VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    UNIQUE INDEX uk_ai_model_packages_code (code)
);

-- =========================================================================
-- 2. BẢNG NGƯỜI DÙNG (USERS) - Tích hợp OAuth2 & Thông tin bổ sung
-- =========================================================================
CREATE TABLE IF NOT EXISTS users (
    user_id CHAR(36) PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    agency VARCHAR(255),
    role VARCHAR(50) NOT NULL,
    package_id BIGINT,
    expire_date DATETIME,
    affiliate_code VARCHAR(255),
    affiliate_link VARCHAR(255),
    total_earnings DOUBLE,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    last_login_at DATETIME,
    provider VARCHAR(20) DEFAULT 'LOCAL' NULL COMMENT 'LOCAL, GOOGLE, FACEBOOK...', -- Đã đổi thành NULL
    provider_id VARCHAR(255) COMMENT 'ID từ Google/Facebook',
    avatar_url VARCHAR(500) COMMENT 'Ảnh đại diện từ Google',
    password_reset_token VARCHAR(255),
    verification_token VARCHAR(255) COMMENT 'Token xác minh email',
    phone VARCHAR(32) NULL,
    position VARCHAR(255) NULL,
    UNIQUE INDEX idx_users_email (email),
    INDEX idx_users_provider (provider),
    INDEX idx_users_provider_id (provider_id),
    INDEX idx_users_verification_token (verification_token),
    CONSTRAINT fk_users_package FOREIGN KEY (package_id) REFERENCES admin_packages(id) ON DELETE SET NULL
);

-- =========================================================================
-- 3. BẢNG TRỢ LÝ (ASSISTANTS)
-- =========================================================================
CREATE TABLE IF NOT EXISTS assistants (
    assistant_id INT PRIMARY KEY,
    assistant_name VARCHAR(255) NOT NULL UNIQUE
);

-- =========================================================================
-- 4. BẢNG PHIÊN CHAT (CHAT_SESSIONS)
-- =========================================================================
CREATE TABLE IF NOT EXISTS chat_sessions (
    session_id INT AUTO_INCREMENT PRIMARY KEY,   
    session_uuid VARCHAR(36) NOT NULL,
    user_id CHAR(36) NOT NULL,
    assistant_id INT NULL, -- Thêm cột liên kết với bảng assistants
    session_name VARCHAR(255) NOT NULL DEFAULT 'Phiên làm việc mới',
    tag_id VARCHAR(36),
    status VARCHAR(50) DEFAULT 'DRAFT',
    wizard_state_json JSON,
    chat_history_json JSON,
    editor_content LONGTEXT,
    html_content LONGTEXT,
    export_format VARCHAR(50),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_chat_sessions_assistant FOREIGN KEY (assistant_id) REFERENCES assistants(assistant_id) ON DELETE SET NULL,
    INDEX idx_chat_sessions_user_id (user_id),
    INDEX idx_chat_sessions_assistant_id (assistant_id)
);

-- =========================================================================
-- 5. BẢNG MÃ LÀM MỚI TOKEN (REFRESH_TOKENS)
-- =========================================================================
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    token VARCHAR(255) NOT NULL,
    expiry_date DATETIME NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    UNIQUE INDEX idx_refresh_tokens_token (token)
);

-- =========================================================================
-- 6. BẢNG HÓA ĐƠN THANH TOÁN (BILLING_INVOICES) - Đã tích hợp QR & Snapshot
-- =========================================================================
CREATE TABLE IF NOT EXISTS billing_invoices (
    invoice_id CHAR(36) PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    package_id BIGINT,
    duration_months INT,
    original_amount BIGINT,
    discount_amount BIGINT,
    final_amount BIGINT,
    memo_id VARCHAR(255),
    qr_code_url VARCHAR(2048) NULL,
    qr_bank_snapshot JSON NULL,
    status VARCHAR(50),
    invoice_type VARCHAR(50) NULL,
    package_snapshot TEXT NULL,
    external_transaction_id VARCHAR(255) NULL,
    created_at DATETIME NOT NULL,
    payment_date DATETIME,
    INDEX idx_billing_invoices_user_id (user_id),
    UNIQUE INDEX uk_billing_invoice_external_tx (external_transaction_id),
    CONSTRAINT fk_billing_invoices_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_billing_invoices_package FOREIGN KEY (package_id) REFERENCES admin_packages(id) ON DELETE SET NULL
);

-- =========================================================================
-- 7. BẢNG GIAO DỊCH (TRANSACTIONS)
-- =========================================================================
CREATE TABLE IF NOT EXISTS transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    invoice_id VARCHAR(255) NOT NULL,
    memo_id VARCHAR(255) NOT NULL,
    amount DECIMAL(38, 2) NOT NULL,
    status VARCHAR(50) DEFAULT 'PENDING',
    external_transaction_id VARCHAR(255) DEFAULT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    CONSTRAINT fk_transactions_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    UNIQUE INDEX uk_transactions_external_tx (external_transaction_id)
);

-- =========================================================================
-- 8. BẢNG NHẬT KÝ GIAO DỊCH CREDITS (CREDIT_TRANSACTIONS)
-- =========================================================================
CREATE TABLE IF NOT EXISTS credit_transactions (

    transaction_id CHAR(36) PRIMARY KEY,

    user_id CHAR(36) NOT NULL,

    -- AI usage
    model VARCHAR(255),
    model_package_id BIGINT,
    prompt_tokens INT,
    completion_tokens INT,
    total_tokens INT,

    -- Credit pricing snapshot
    input_credit DOUBLE,
    output_credit DOUBLE,
    credit_rate DOUBLE,
    output_weight DOUBLE,

    -- Credit lifecycle
    total_credit_hold DOUBLE,
    actual_credit_deducted DOUBLE,
    refunded_credit DOUBLE,

    -- Transaction info
    type VARCHAR(50),
    description LONGTEXT,

    created_at DATETIME NOT NULL,

    INDEX idx_credit_transactions_user_id (user_id),
    INDEX idx_credit_transactions_model_package_id (model_package_id),

    CONSTRAINT fk_credit_transactions_user
        FOREIGN KEY (user_id)
        REFERENCES users(user_id)
        ON DELETE CASCADE

);

-- =========================================================================
-- 9. BẢNG TÀI KHOẢN CREDIT (CREDIT_ACCOUNTS)
-- =========================================================================
CREATE TABLE IF NOT EXISTS credit_accounts (
  user_id CHAR(36) NOT NULL PRIMARY KEY,
  monthly_quota_allocated DOUBLE NOT NULL DEFAULT 0,
  monthly_quota_remaining DOUBLE NOT NULL DEFAULT 0,
  monthly_quota_cycle_start DATETIME DEFAULT NULL,
  monthly_quota_cycle_end DATETIME DEFAULT NULL,
  purchased_credit_balance DOUBLE NOT NULL DEFAULT 0,
  purchased_credit_purchased_at DATETIME DEFAULT NULL,
  purchased_credit_expire_at DATETIME DEFAULT NULL,
  CONSTRAINT fk_credit_accounts_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- =========================================================================
-- 10. BẢNG QUẢN LÝ TẬP TIN UPLOAD (FILES) - Đã tích hợp trích xuất metadata
-- =========================================================================
CREATE TABLE IF NOT EXISTS files (
    file_id CHAR(36) PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_url VARCHAR(255) NOT NULL,
    file_size BIGINT,
    category VARCHAR(50) NOT NULL DEFAULT 'INPUT_DIRECTIVE',
    mime_type VARCHAR(255),
    raw_text LONGTEXT NULL COMMENT 'Nội dung gốc sau khi extract',
    normalized_text LONGTEXT NULL COMMENT 'Nội dung sau khi normalize',
    word_count INT NOT NULL DEFAULT 0 COMMENT 'Số lượng từ',
    character_count INT NOT NULL DEFAULT 0 COMMENT 'Số lượng ký tự',
    extraction_status VARCHAR(30) NOT NULL DEFAULT 'UPLOADED' COMMENT 'Trạng thái xử lý file',
    extracted_at DATETIME NULL COMMENT 'Thời điểm hoàn thành extract',
    uploaded_at DATETIME NOT NULL,
    INDEX idx_files_user_id (user_id),
    CONSTRAINT fk_files_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- =========================================================================
-- 11. BẢNG LIÊN KẾT PHIÊN CHAT VÀ FILE (CHAT_SESSION_FILES)
-- =========================================================================
CREATE TABLE IF NOT EXISTS chat_session_files (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id INT NOT NULL,
    file_id CHAR(36) NOT NULL,
    field_code VARCHAR(100) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_session_file_field UNIQUE (session_id, file_id, field_code),
    CONSTRAINT fk_chat_session_files_session FOREIGN KEY (session_id) REFERENCES chat_sessions(session_id) ON DELETE CASCADE,
    CONSTRAINT fk_chat_session_files_file FOREIGN KEY (file_id) REFERENCES files(file_id) ON DELETE CASCADE
);

-- =========================================================================
-- 12. BẢNG THƯ VIỆN TÀI LIỆU (DOCUMENT_LIBRARY)
-- =========================================================================
CREATE TABLE IF NOT EXISTS document_library (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id CHAR(36) NOT NULL,
    session_id CHAR(36) DEFAULT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_url VARCHAR(512) DEFAULT NULL,
    category VARCHAR(100) DEFAULT NULL,
    extracted_text LONGTEXT DEFAULT NULL,
    file_size BIGINT DEFAULT NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_doc_library_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- =========================================================================
-- 13. BẢNG CẤU HÌNH NGÂN HÀNG THANH TOÁN (PAYMENT_BANK_CONFIG)
-- =========================================================================
CREATE TABLE IF NOT EXISTS payment_bank_config (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  bank_code VARCHAR(64),
  account_number VARCHAR(128),
  account_name VARCHAR(255),
  va_number VARCHAR(128),
  template TEXT,
  show_info BOOLEAN DEFAULT TRUE,
  store VARCHAR(255),
  is_active BOOLEAN DEFAULT FALSE,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- =========================================================================
-- 14. BẢNG THỐNG KÊ HỆ THỐNG (SYSTEM_STATS)
-- =========================================================================
CREATE TABLE IF NOT EXISTS system_stats (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    total_revenue BIGINT DEFAULT 0,
    new_users_count BIGINT DEFAULT 0,
    active_affiliates BIGINT DEFAULT 0,
    total_credit_consumed DOUBLE DEFAULT 0.0,
    active_sessions_count BIGINT DEFAULT 0,
    total_documents_generated BIGINT DEFAULT 0,
    updated_at DATETIME NOT NULL
);