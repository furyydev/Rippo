CREATE TABLE chat_messages (
    id BIGSERIAL PRIMARY KEY,
    chat_session_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_chat_messages_chat_session
        FOREIGN KEY (chat_session_id)
        REFERENCES chat_sessions (id)
        ON DELETE CASCADE,
    CONSTRAINT chk_chat_messages_role
        CHECK (role IN ('USER', 'ASSISTANT'))
);

CREATE INDEX idx_chat_messages_chat_session_id ON chat_messages (chat_session_id);
