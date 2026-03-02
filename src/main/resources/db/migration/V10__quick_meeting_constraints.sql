-- 1) 게스트 UUID 유니크 (게스트만)
CREATE UNIQUE INDEX IF NOT EXISTS ux_members_guest_uuid_only_guest
    ON members (guest_uuid)
    WHERE is_guest = true;