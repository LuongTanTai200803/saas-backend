ALTER TABLE files
    DROP INDEX uk_files_user_content_hash;

CREATE INDEX idx_files_user_content_hash
    ON files (user_id, content_hash);