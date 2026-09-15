INSERT INTO ai_model_packages (
    id,
    code,
    name,
    credit_rate,
    models,
    description,
    active,
    created_at,
    updated_at
)
VALUES
    (
        1,
        'BASIC',
        'Basic AI Models',
        1.0,
        '["gpt-4o-mini","claude-3-haiku"]',
        'Các model cơ bản',
        TRUE,
        NOW(),
        NOW()
    ),
    (
        2,
        'STANDARD',
        'Standard AI Models',
        1.5,
        '["gpt-4o-mini","gpt-4o","claude-3-haiku"]',
        'Các model tiêu chuẩn',
        TRUE,
        NOW(),
        NOW()
    ),
    (
        3,
        'PREMIUM',
        'Premium AI Models',
        2.0,
        '["gpt-4o","claude-3-5-sonnet","gemini-1.5-pro"]',
        'Các model cao cấp',
        TRUE,
        NOW(),
        NOW()
    )
ON DUPLICATE KEY UPDATE
    code = VALUES(code),
    name = VALUES(name),
    credit_rate = VALUES(credit_rate),
    models = VALUES(models),
    description = VALUES(description),
    active = VALUES(active),
    updated_at = NOW();
INSERT INTO admin_packages (
    package_type,
    package_category,
    price,
    credit_limit,
    duration,
    model_package_level,
    description,
    storage_quota_mb,
    created_at,
    updated_at
)
VALUES
    (
        'FREE',
        'SUBSCRIPTION',
        0,
        50.0,
        30,
        1,
        'Gói dùng thử miễn phí cho người dùng mới',
        100,
        NOW(),
        NOW()
    ),
    (
        'STANDARD',
        'SUBSCRIPTION',
        199000,
        500.0,
        30,
        2,
        'Gói tiêu chuẩn phù hợp cá nhân',
        2048,
        NOW(),
        NOW()
    ),
    (
        'PREMIUM',
        'SUBSCRIPTION',
        499000,
        1500.0,
        30,
        3,
        'Gói cao cấp không giới hạn tính năng',
        10240,
        NOW(),
        NOW()
    ),
    (
        'CREDIT_500',
        'CREDIT_PACK',
        100000,
        500.0,
        30,
        NULL,
        'One-time credit 500',
        100,
        NOW(),
        NOW()
    ),
    (
        'CREDIT_1000',
        'CREDIT_PACK',
        180000,
        1000.0,
        30,
        NULL,
        'One-time credit 1000',
        100,
        NOW(),
        NOW()
    )
ON DUPLICATE KEY UPDATE
    package_category = VALUES(package_category),
    price = VALUES(price),
    credit_limit = VALUES(credit_limit),
    duration = VALUES(duration),
    model_package_level = VALUES(model_package_level),
    description = VALUES(description),
    storage_quota_mb = VALUES(storage_quota_mb),
    updated_at = NOW();

INSERT INTO assistants (assistant_id, assistant_name)
VALUES
    (1, 'Trợ lý Văn Bản Đảng'),
    (2, 'Trợ lý Văn bản Nhà nước'),
    (3, 'Trợ lý Quản lý Giáo dục'),
    (4, 'Trợ lý Biên tập'),
    (5, 'Trợ lý Rút gọn Kiểm tra'),
    (6, 'Trợ lý Soạn giáo án'),
    (7, 'Trợ lý Đề kiểm tra'),
    (8, 'Trợ lý Đánh giá'),
    (9, 'Trợ lý Viết báo cáo thành tích')
ON DUPLICATE KEY UPDATE
    assistant_name = VALUES(assistant_name);