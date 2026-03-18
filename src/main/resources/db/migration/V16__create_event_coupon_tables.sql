CREATE SEQUENCE events_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE event_participants_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE event_coupons_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE events (
    id BIGINT PRIMARY KEY DEFAULT nextval('events_seq'),
    title VARCHAR(100) NOT NULL,
    description TEXT NOT NULL,
    coupon_type VARCHAR(30) NOT NULL,
    start_at TIMESTAMPTZ NOT NULL,
    end_at TIMESTAMPTZ NOT NULL,
    capacity INT NOT NULL,
    issued_count INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ck_events_capacity_non_negative
        CHECK (capacity >= 0),
    CONSTRAINT ck_events_issued_count_non_negative
        CHECK (issued_count >= 0),
    CONSTRAINT ck_events_issued_count_within_capacity
        CHECK (issued_count <= capacity),
    CONSTRAINT ck_events_period
        CHECK (start_at < end_at)
);

CREATE INDEX idx_events_active_period
    ON events (is_active, is_deleted, start_at, end_at);

CREATE INDEX idx_events_created_at
    ON events (created_at DESC, id DESC);

DROP TRIGGER IF EXISTS trg_events_set_updated_at ON events;
CREATE TRIGGER trg_events_set_updated_at
    BEFORE UPDATE ON events
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


CREATE TABLE event_participants (
    id BIGINT PRIMARY KEY DEFAULT nextval('event_participants_seq'),
    event_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_event_participants_event
        FOREIGN KEY (event_id) REFERENCES events(id) ON DELETE CASCADE,
    CONSTRAINT fk_event_participants_member
        FOREIGN KEY (member_id) REFERENCES members(id) ON DELETE CASCADE,
    CONSTRAINT uq_event_participants_event_member
        UNIQUE (event_id, member_id)
);

CREATE INDEX idx_event_participants_member_created
    ON event_participants (member_id, created_at DESC, id DESC);

CREATE INDEX idx_event_participants_event_created
    ON event_participants (event_id, created_at DESC, id DESC);

DROP TRIGGER IF EXISTS trg_event_participants_set_updated_at ON event_participants;
CREATE TRIGGER trg_event_participants_set_updated_at
    BEFORE UPDATE ON event_participants
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();


CREATE TABLE event_coupons (
    id BIGINT PRIMARY KEY DEFAULT nextval('event_coupons_seq'),
    event_participant_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    coupon_type VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ISSUED',
    used_at TIMESTAMPTZ NULL,
    expired_at TIMESTAMPTZ NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_event_coupons_event_participant
        FOREIGN KEY (event_participant_id) REFERENCES event_participants(id) ON DELETE CASCADE,
    CONSTRAINT fk_event_coupons_member
        FOREIGN KEY (member_id) REFERENCES members(id) ON DELETE CASCADE,
    CONSTRAINT uq_event_coupons_event_participant
        UNIQUE (event_participant_id),
    CONSTRAINT ck_event_coupons_status
        CHECK (status IN ('ISSUED', 'USED', 'EXPIRED'))
);

CREATE INDEX idx_event_coupons_member_status_expired
    ON event_coupons (member_id, status, expired_at, created_at DESC, id DESC);

CREATE INDEX idx_event_coupons_member_created
    ON event_coupons (member_id, created_at DESC, id DESC);

CREATE INDEX idx_event_coupons_expired_at
    ON event_coupons (expired_at);

DROP TRIGGER IF EXISTS trg_event_coupons_set_updated_at ON event_coupons;
CREATE TRIGGER trg_event_coupons_set_updated_at
    BEFORE UPDATE ON event_coupons
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();