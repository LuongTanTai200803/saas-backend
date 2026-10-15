ALTER TABLE files
    ADD COLUMN content_hash VARCHAR(64) NULL,
    ADD COLUMN extraction_error LONGTEXT NULL;
    
ALTER TABLE files
    ADD CONSTRAINT uk_files_user_content_hash
    UNIQUE (user_id, content_hash);