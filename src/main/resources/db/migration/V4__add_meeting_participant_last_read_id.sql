ALTER TABLE meeting_participants
    ADD COLUMN IF NOT EXISTS last_read_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_meeting_participants_meeting_status_user
    ON meeting_participants (meeting_id, status, member_id);

CREATE INDEX IF NOT EXISTS idx_meeting_participants_meeting_last_read_id
    ON meeting_participants (meeting_id, last_read_id);
