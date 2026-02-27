/* ----------------------------------------------------------------------
 * 1) restaurants 테이블 컬럼 변경
 * ---------------------------------------------------------------------- */

-- lat/lng 타입 변경: numeric(38,2) -> numeric(10,7)
ALTER TABLE restaurants
  ALTER COLUMN lat TYPE numeric(10,7) USING lat::numeric(10,7),
  ALTER COLUMN lng TYPE numeric(10,7) USING lng::numeric(10,7);

-- image_url1/2/3 -> image_1/2/3 (이름 변경 + 타입 text로 변경)
ALTER TABLE restaurants
  RENAME COLUMN image_url1 TO image_1;

ALTER TABLE restaurants
  RENAME COLUMN image_url2 TO image_2;

ALTER TABLE restaurants
  RENAME COLUMN image_url3 TO image_3;

ALTER TABLE restaurants
  ALTER COLUMN image_1 TYPE text,
  ALTER COLUMN image_2 TYPE text,
  ALTER COLUMN image_3 TYPE text;

-- 새 컬럼 추가: source_hash text
ALTER TABLE restaurants
  ADD COLUMN source_hash text;


/* ----------------------------------------------------------------------
 * 2) restaurants_docs_hashes 테이블 생성
 * ---------------------------------------------------------------------- */

CREATE TABLE restaurants_docs_hashes (
  place_id              BIGINT PRIMARY KEY
    REFERENCES restaurants(id),

  profile_hash          text NULL,
  menu_hash             text NULL,
  hours_hash            text NULL,
  review_evidence_hash  text NULL,

  created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at            TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS trigger AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_restaurants_docs_hashes_set_updated_at
BEFORE UPDATE ON restaurants_docs_hashes
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();