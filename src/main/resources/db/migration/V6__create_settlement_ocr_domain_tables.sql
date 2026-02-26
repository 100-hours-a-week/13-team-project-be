-- 정산(OCR) 도메인
-- meeting_settlements / receipt_items / settlement_participants / settlement_item_selections

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 1) sequences
CREATE SEQUENCE meeting_settlements_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE receipt_items_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE settlement_participants_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE settlement_item_selections_seq START WITH 1 INCREMENT BY 1;

-- 2) meeting_settlements (모임 정산: 모임당 1개 세션)
CREATE TABLE meeting_settlements (
  id                  BIGINT PRIMARY KEY DEFAULT nextval('meeting_settlements_seq'),
  meeting_id           BIGINT NOT NULL REFERENCES meetings(id),
  receipt_image_url    VARCHAR(500) NULL,
  settlement_status    VARCHAR(30) NOT NULL,
  discount_amount      NUMERIC(12,2) NULL,
  total_amount         NUMERIC(12,2) NULL,
  created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),

  CONSTRAINT uq_meeting_settlements_meeting UNIQUE (meeting_id)
);

CREATE INDEX idx_meeting_settlements_meeting_id
  ON meeting_settlements (meeting_id);

DROP TRIGGER IF EXISTS trg_meeting_settlements_set_updated_at ON meeting_settlements;
CREATE TRIGGER trg_meeting_settlements_set_updated_at
BEFORE UPDATE ON meeting_settlements
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- 3) receipt_items (영수증 항목)
CREATE TABLE receipt_items (
  id            BIGINT PRIMARY KEY DEFAULT nextval('receipt_items_seq'),
  settlement_id BIGINT NOT NULL REFERENCES meeting_settlements(id) ON DELETE CASCADE,
  item_name     VARCHAR(100) NOT NULL,
  unit_price    NUMERIC(12,2) NULL,
  quantity      INT NULL,
  total_price   NUMERIC(12,2) NOT NULL,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_receipt_items_settlement_id
  ON receipt_items (settlement_id);

-- 4) settlement_participants (참여자별 정산 정보)
CREATE TABLE settlement_participants (
  id                        BIGINT PRIMARY KEY DEFAULT nextval('settlement_participants_seq'),
  settlement_id             BIGINT NOT NULL REFERENCES meeting_settlements(id) ON DELETE CASCADE,
  participant_id            BIGINT NOT NULL REFERENCES meeting_participants(id),

  subtotal_amount           NUMERIC(12,2) NULL,
  discount_allocated_amount NUMERIC(12,2) NULL,
  amount_due                NUMERIC(12,2) NULL,

  payment_status            VARCHAR(20) NOT NULL DEFAULT 'UNPAID',
  selection_confirmed_at    TIMESTAMPTZ NULL,
  payment_requested_at      TIMESTAMPTZ NULL,
  payment_confirmed_at      TIMESTAMPTZ NULL,

  created_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at                TIMESTAMPTZ NOT NULL DEFAULT now(),

  CONSTRAINT uq_settlement_participants UNIQUE (settlement_id, participant_id),
  CONSTRAINT ck_settlement_payment_status CHECK (payment_status IN ('UNPAID','REQUESTED','DONE'))
);

CREATE INDEX idx_settlement_participants_settlement_id
  ON settlement_participants (settlement_id);

CREATE INDEX idx_settlement_participants_settlement_payment_status
  ON settlement_participants (settlement_id, payment_status);

DROP TRIGGER IF EXISTS trg_settlement_participants_set_updated_at ON settlement_participants;
CREATE TRIGGER trg_settlement_participants_set_updated_at
BEFORE UPDATE ON settlement_participants
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- 5) settlement_item_selections (누가 어떤 메뉴를 먹었는지)
CREATE TABLE settlement_item_selections (
  id                       BIGINT PRIMARY KEY DEFAULT nextval('settlement_item_selections_seq'),
  item_id                  BIGINT NOT NULL REFERENCES receipt_items(id) ON DELETE CASCADE,
  settlement_participant_id BIGINT NOT NULL REFERENCES settlement_participants(id) ON DELETE CASCADE,
  created_at               TIMESTAMPTZ NOT NULL DEFAULT now(),

  CONSTRAINT uq_settlement_item_selections UNIQUE (settlement_participant_id, item_id)
);

CREATE INDEX idx_settlement_item_selections_participant_id
  ON settlement_item_selections (settlement_participant_id);

CREATE INDEX idx_settlement_item_selections_item_id
  ON settlement_item_selections (item_id);