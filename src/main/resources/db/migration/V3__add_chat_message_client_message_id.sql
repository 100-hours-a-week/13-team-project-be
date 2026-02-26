ALTER TABLE chat_messages
    ADD COLUMN IF NOT EXISTS client_message_id VARCHAR(64);

CREATE UNIQUE INDEX IF NOT EXISTS uq_chat_messages_idempotency
    ON chat_messages (meeting_id, participant_id, client_message_id)
    WHERE client_message_id IS NOT NULL;
