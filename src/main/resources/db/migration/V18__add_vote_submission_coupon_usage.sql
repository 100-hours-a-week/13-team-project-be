ALTER TABLE vote_submissions
    ADD COLUMN used_coupon_id BIGINT NULL,
    ADD COLUMN vote_weight INT NOT NULL DEFAULT 1;

ALTER TABLE vote_submissions
    ADD CONSTRAINT fk_vote_submissions_used_coupon
        FOREIGN KEY (used_coupon_id) REFERENCES event_coupons(id);

ALTER TABLE vote_submissions
    ADD CONSTRAINT ck_vote_submissions_vote_weight_positive
        CHECK (vote_weight > 0);

CREATE INDEX idx_vote_submissions_used_coupon_id
    ON vote_submissions (used_coupon_id);
