-- PostgreSQL schema for matchimban-api (JPA entity-based)

-- sequences
CREATE SEQUENCE members_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE oauth_accounts_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE food_category_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE member_category_mapping_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE member_agreements_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE policy_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE meetings_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE meeting_participants_seq START WITH 1 INCREMENT BY 50;

-- members
CREATE TABLE members (
    id BIGINT PRIMARY KEY DEFAULT nextval('members_seq'),
    nickname VARCHAR(30),
    profile_image_url VARCHAR(500),
    thumbnail_image_url VARCHAR(500),
    status VARCHAR(10),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    is_guest BOOLEAN NOT NULL,
    guest_uuid UUID
);

-- policy
CREATE TABLE policy (
    id BIGINT PRIMARY KEY DEFAULT nextval('policy_seq'),
    terms_content TEXT NOT NULL,
    policy_type VARCHAR(30) NOT NULL,
    title VARCHAR(100) NOT NULL,
    terms_version VARCHAR(10) NOT NULL,
    summary TEXT,
    created_at TIMESTAMP NOT NULL,
    is_required BOOLEAN NOT NULL,
    CONSTRAINT uk_policy_type_version UNIQUE (policy_type, terms_version)
);

-- food_category
CREATE TABLE food_category (
    id BIGINT PRIMARY KEY DEFAULT nextval('food_category_seq'),
    category_code VARCHAR(30) NOT NULL,
    category_name VARCHAR(30) NOT NULL,
    emoji VARCHAR(10),
    category_type VARCHAR(20) NOT NULL,
    CONSTRAINT uk_food_category_code_type UNIQUE (category_code, category_type)
);

-- oauth_accounts
CREATE TABLE oauth_accounts (
    id BIGINT PRIMARY KEY DEFAULT nextval('oauth_accounts_seq'),
    created_at TIMESTAMP,
    provider_member_id VARCHAR(32) NOT NULL,
    provider VARCHAR(10) NOT NULL,
    member_id BIGINT NOT NULL,
    CONSTRAINT fk_oauth_accounts_member
        FOREIGN KEY (member_id) REFERENCES members(id)
);

-- member_agreements
CREATE TABLE member_agreements (
    id BIGINT PRIMARY KEY DEFAULT nextval('member_agreements_seq'),
    member_id BIGINT NOT NULL,
    policy_id BIGINT NOT NULL,
    accepted_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_member_agreements_member
        FOREIGN KEY (member_id) REFERENCES members(id),
    CONSTRAINT fk_member_agreements_policy
        FOREIGN KEY (policy_id) REFERENCES policy(id)
);

-- member_category_mapping
CREATE TABLE member_category_mapping (
    id BIGINT PRIMARY KEY DEFAULT nextval('member_category_mapping_seq'),
    relation_type VARCHAR(10),
    category_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    CONSTRAINT fk_member_category_mapping_category
        FOREIGN KEY (category_id) REFERENCES food_category(id),
    CONSTRAINT fk_member_category_mapping_member
        FOREIGN KEY (member_id) REFERENCES members(id)
);

-- meetings
CREATE TABLE meetings (
    id BIGINT PRIMARY KEY DEFAULT nextval('meetings_seq'),
    title VARCHAR(20) NOT NULL,
    scheduled_at TIMESTAMP NOT NULL,
    location_address VARCHAR(255) NOT NULL,
    location_lat NUMERIC(10, 7) NOT NULL,
    location_lng NUMERIC(10, 7) NOT NULL,
    target_headcount INTEGER NOT NULL,
    search_radius_m INTEGER NOT NULL,
    vote_deadline_at TIMESTAMP NOT NULL,
    is_except_meat BOOLEAN NOT NULL,
    is_except_bar BOOLEAN NOT NULL,
    swipe_count INTEGER NOT NULL,
    is_quick_meeting BOOLEAN NOT NULL,
    invite_code VARCHAR(8) NOT NULL,
    last_chat_id BIGINT,
    is_deleted BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    host_member_id BIGINT NOT NULL
);

-- meeting_participants
CREATE TABLE meeting_participants (
    id BIGINT PRIMARY KEY DEFAULT nextval('meeting_participants_seq'),
    meeting_id BIGINT NOT NULL,
    member_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    last_read_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_meeting_participants_meeting
        FOREIGN KEY (meeting_id) REFERENCES meetings(id),
    CONSTRAINT fk_meeting_participants_member
        FOREIGN KEY (member_id) REFERENCES members(id)
);
