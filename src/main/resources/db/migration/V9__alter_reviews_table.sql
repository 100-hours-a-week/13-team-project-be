
CREATE SEQUENCE IF NOT EXISTS reviews_seq START WITH 1 INCREMENT BY 1;

ALTER TABLE reviews
  ALTER COLUMN id SET DEFAULT nextval('reviews_seq');

ALTER SEQUENCE reviews_seq OWNED BY reviews.id;

SELECT setval(
  'reviews_seq',
  COALESCE((SELECT MAX(id) FROM reviews), 0) + 1,
  false
);

ALTER TABLE reviews ADD COLUMN IF NOT EXISTS meeting_id BIGINT;
ALTER TABLE reviews ADD COLUMN IF NOT EXISTS member_id BIGINT;

UPDATE reviews r
SET meeting_id = mp.meeting_id,
    member_id  = mp.member_id
FROM meeting_participants mp
WHERE r.participant_id = mp.id
  AND (r.meeting_id IS NULL OR r.member_id IS NULL);

DO $$
DECLARE
  v_cnt BIGINT;
BEGIN
  SELECT COUNT(*) INTO v_cnt
  FROM reviews
  WHERE meeting_id IS NULL OR member_id IS NULL;

  IF v_cnt > 0 THEN
    RAISE EXCEPTION 'V9 migration failed: reviews.meeting_id/member_id backfill incomplete. remaining rows=%', v_cnt;
  END IF;
END $$;

UPDATE reviews SET is_deleted = FALSE WHERE is_deleted IS NULL;
UPDATE reviews SET content = '' WHERE content IS NULL;
UPDATE reviews SET rating = 1 WHERE rating IS NULL;

ALTER TABLE reviews
  ALTER COLUMN rating TYPE SMALLINT
  USING round(rating::numeric)::smallint;

ALTER TABLE reviews
  ALTER COLUMN content TYPE TEXT;

UPDATE reviews
SET rating = LEAST(5, GREATEST(1, rating));

ALTER TABLE reviews
  ALTER COLUMN meeting_id SET NOT NULL,
  ALTER COLUMN member_id SET NOT NULL,
  ALTER COLUMN restaurant_id SET NOT NULL,
  ALTER COLUMN rating SET NOT NULL,
  ALTER COLUMN content SET NOT NULL,
  ALTER COLUMN is_deleted SET DEFAULT FALSE,
  ALTER COLUMN is_deleted SET NOT NULL;

UPDATE reviews SET created_at = CURRENT_TIMESTAMP WHERE created_at IS NULL;
UPDATE reviews SET updated_at = CURRENT_TIMESTAMP WHERE updated_at IS NULL;

ALTER TABLE reviews
  ALTER COLUMN created_at SET NOT NULL,
  ALTER COLUMN updated_at SET NOT NULL;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1
    FROM pg_constraint c
    WHERE c.conrelid = 'reviews'::regclass
      AND c.contype = 'c'
      AND c.conname = 'ck_reviews_rating_range'
  ) THEN
    ALTER TABLE reviews
      ADD CONSTRAINT ck_reviews_rating_range CHECK (rating BETWEEN 1 AND 5);
  END IF;
END $$;

DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_reviews_meeting') THEN
    ALTER TABLE reviews
      ADD CONSTRAINT fk_reviews_meeting
      FOREIGN KEY (meeting_id) REFERENCES meetings(id);
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_reviews_member') THEN
    ALTER TABLE reviews
      ADD CONSTRAINT fk_reviews_member
      FOREIGN KEY (member_id) REFERENCES members(id);
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_reviews_restaurant') THEN
    ALTER TABLE reviews
      ADD CONSTRAINT fk_reviews_restaurant
      FOREIGN KEY (restaurant_id) REFERENCES restaurants(id);
  END IF;
END $$;

ALTER TABLE reviews DROP CONSTRAINT IF EXISTS uk_reviews_participant_restaurant;

DROP INDEX IF EXISTS uq_reviews_participant_restaurant_active;
DROP INDEX IF EXISTS uk_reviews_participant_restaurant;

CREATE UNIQUE INDEX IF NOT EXISTS uq_reviews_meeting_member_active
  ON reviews (meeting_id, member_id)
  WHERE is_deleted = FALSE;

CREATE INDEX IF NOT EXISTS idx_reviews_member_id_id
  ON reviews (member_id, id DESC)
  WHERE is_deleted = FALSE;

ALTER TABLE reviews DROP CONSTRAINT IF EXISTS fk_reviews_participant;
ALTER TABLE reviews DROP COLUMN IF EXISTS participant_id;