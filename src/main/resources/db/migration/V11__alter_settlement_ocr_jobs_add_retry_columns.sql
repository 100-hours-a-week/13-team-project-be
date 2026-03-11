ALTER TABLE settlement_ocr_jobs
    ADD COLUMN next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN started_at TIMESTAMPTZ NULL,
    ADD COLUMN completed_at TIMESTAMPTZ NULL;

CREATE INDEX idx_settlement_ocr_jobs_claimable
    ON settlement_ocr_jobs (status, next_attempt_at, lock_until);