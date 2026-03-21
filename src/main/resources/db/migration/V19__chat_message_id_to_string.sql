-- MongoDB migration: change lastReadId and lastChatId from bigint to varchar(24) for ObjectId
ALTER TABLE meeting_participants
    ALTER COLUMN last_read_id TYPE VARCHAR(24)
    USING CASE WHEN last_read_id IS NOT NULL THEN last_read_id::TEXT ELSE NULL END;

ALTER TABLE meetings
    ALTER COLUMN last_chat_id TYPE VARCHAR(24)
    USING CASE WHEN last_chat_id IS NOT NULL THEN last_chat_id::TEXT ELSE NULL END;
