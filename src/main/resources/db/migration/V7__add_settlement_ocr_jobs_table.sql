CREATE SEQUENCE settlement_ocr_jobs_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE settlement_ocr_jobs (
  id              BIGINT PRIMARY KEY DEFAULT nextval('settlement_ocr_jobs_seq'),
  settlement_id   BIGINT NOT NULL REFERENCES meeting_settlements(id) ON DELETE CASCADE,

  request_id      VARCHAR(80) NOT NULL,
  status          VARCHAR(20) NOT NULL, -- PENDING/PROCESSING/SUCCEEDED/FAILED

  attempt_count   INT NOT NULL DEFAULT 0,

  locked_by       VARCHAR(120) NULL,
  lock_until      TIMESTAMPTZ NULL,

  last_error_code    VARCHAR(100) NULL,
  last_error_message VARCHAR(500) NULL,

  created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_settlement_ocr_jobs_status_lock
  ON settlement_ocr_jobs (status, lock_until);

CREATE INDEX idx_settlement_ocr_jobs_settlement_id
  ON settlement_ocr_jobs (settlement_id);

CREATE UNIQUE INDEX uq_settlement_ocr_jobs_active
  ON settlement_ocr_jobs (settlement_id)
  WHERE status IN ('PENDING', 'PROCESSING');

DROP TRIGGER IF EXISTS trg_settlement_ocr_jobs_set_updated_at ON settlement_ocr_jobs;
CREATE TRIGGER trg_settlement_ocr_jobs_set_updated_at
BEFORE UPDATE ON settlement_ocr_jobs
FOR EACH ROW EXECUTE FUNCTION set_updated_at();