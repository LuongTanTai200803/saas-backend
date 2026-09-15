-- =========================================================
-- V3: Bắt buộc mỗi ChatSession phải thuộc một Assistant
-- =========================================================

START TRANSACTION;

-- 1. Cập nhật dữ liệu cũ nếu đang bị thiếu assistant_id
UPDATE chat_sessions
SET assistant_id = (
    SELECT MIN(assistant_id)
    FROM assistants
)
WHERE assistant_id IS NULL
  AND EXISTS (
      SELECT 1
      FROM assistants
  );

-- 2. Xóa khóa ngoại cũ (đang mang thuộc tính SET NULL)
ALTER TABLE chat_sessions
    DROP FOREIGN KEY fk_chat_sessions_assistant;

-- 3. Đổi cột thành NOT NULL (lúc này đã an toàn vì không còn giá trị NULL)
ALTER TABLE chat_sessions
    MODIFY assistant_id INT NOT NULL;

-- 4. Tạo lại khóa ngoại với hành vi ON DELETE RESTRICT (hoặc CASCADE)
ALTER TABLE chat_sessions
    ADD CONSTRAINT fk_chat_sessions_assistant
    FOREIGN KEY (assistant_id)
    REFERENCES assistants(assistant_id)
    ON DELETE RESTRICT;

-- 5. Đảm bảo unique index cho session_uuid
ALTER TABLE chat_sessions
    ADD UNIQUE INDEX uk_chat_sessions_session_uuid (session_uuid);

COMMIT;