
-- Extend the existing project table without removing existing data.
ALTER TABLE project
ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PLANNED'
    CONSTRAINT ck_project_status
    CHECK (status IN ('PLANNED', 'ACTIVE', 'ON_HOLD', 'COMPLETED', 'CANCELLED')),
ADD COLUMN start_date DATE,
ADD COLUMN end_date DATE,
ADD COLUMN created_by BIGINT REFERENCES member(member_id);

-- Ensure project end dates are not earlier than start dates.
ALTER TABLE project
ADD CONSTRAINT ck_project_dates
CHECK (
    start_date IS NULL
    OR end_date IS NULL
    OR end_date >= start_date
);

-- Backfill the creator for any projects already in the database.
-- The Space creator is used as a fallback because V2 did not track
-- who originally created each project.
UPDATE project p
SET created_by = s.created_by
FROM space s
WHERE p.space_id = s.space_id
  AND p.created_by IS NULL;

ALTER TABLE project
ALTER COLUMN created_by SET NOT NULL;
