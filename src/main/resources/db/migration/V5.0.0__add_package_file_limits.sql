ALTER TABLE admin_packages
    ADD COLUMN max_files_per_call INT NOT NULL DEFAULT 5,
    ADD COLUMN max_file_size_mb BIGINT NOT NULL DEFAULT 10,
    ADD COLUMN max_total_file_size_mb_per_call BIGINT NOT NULL DEFAULT 25,
    ADD COLUMN max_text_per_call BIGINT NOT NULL DEFAULT 100000,
    ADD COLUMN allowed_file_types JSON;
    