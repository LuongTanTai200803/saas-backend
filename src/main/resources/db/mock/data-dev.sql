-- Dữ liệu này chỉ chạy ở máy Local để lập trình viên test Postman
INSERT INTO users (email, password_hash, full_name, agency, role, package_type, expire_date, created_at, updated_at)
VALUES (
    'admin.tai@coquan.gov.vn', 
    '$2a$10$8/Vnyg67YrYQae6YPNHvsOaBMDGt0cPxWL9I.mvhCGmFpYvDfF1LC', -- Mã băm chuẩn của MatKhauManh123@
    'Lương Tấn Tài (Admin)', 
    'Ban Quản Trị Hệ Thống', 
    'ROLE_ADMIN',
    'FREE', 
    '2030-12-31 23:59:59',
    NOW(),
    NOW()
) ON DUPLICATE KEY UPDATE email=email; 
-- Dòng cuối giúp tránh lỗi crash ứng dụng nếu bản ghi admin này đã tồn tại sẵn trong DB