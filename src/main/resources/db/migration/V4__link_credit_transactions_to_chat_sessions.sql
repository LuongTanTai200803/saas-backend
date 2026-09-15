ALTER TABLE credit_transactions
    ADD COLUMN session_id INT NULL;

ALTER TABLE credit_transactions
    ADD INDEX idx_credit_transactions_session_id (session_id);

ALTER TABLE credit_transactions
    ADD CONSTRAINT fk_credit_transactions_session
    FOREIGN KEY (session_id)
    REFERENCES chat_sessions(session_id)
    ON DELETE SET NULL;