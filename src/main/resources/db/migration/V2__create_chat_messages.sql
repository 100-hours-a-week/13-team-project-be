CREATE SEQUENCE IF NOT EXISTS chat_messages_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS chat_messages (
    id BIGINT DEFAULT nextval('chat_messages_seq') PRIMARY KEY,
    meeting_id BIGINT NOT NULL,
    participant_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL DEFAULT 'TEXT',
    message TEXT NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_chat_messages_meeting
        FOREIGN KEY (meeting_id) REFERENCES meetings(id),
    CONSTRAINT fk_chat_messages_participant
        FOREIGN KEY (participant_id) REFERENCES meeting_participants(id)
);

CREATE INDEX IF NOT EXISTS idx_chat_messages_meeting_id_id
    ON chat_messages (meeting_id, id);
