CREATE TABLE IF NOT EXISTS file_ocr_pages (
    id BIGINT NOT NULL AUTO_INCREMENT,
    file_id CHAR(36) NOT NULL,
    page_index INT NOT NULL,
    ocr_text LONGTEXT NULL,
    ocr_status VARCHAR(30) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_file_ocr_page
        UNIQUE (file_id, page_index),
    CONSTRAINT fk_file_ocr_page_file
        FOREIGN KEY (file_id)
        REFERENCES files(file_id)
        ON DELETE CASCADE
);