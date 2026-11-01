-- ProjectFlow core schema
-- Requires PostgreSQL 17 or later.
-- This migration assumes V1 did not create these application tables.

CREATE TABLE member (
    member_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    cognito_sub VARCHAR(255) UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE space (
    space_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    created_by BIGINT NOT NULL REFERENCES member(member_id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE project (
    project_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    space_id BIGINT NOT NULL REFERENCES space(space_id),
    name VARCHAR(150) NOT NULL,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Each project owns its Kanban statuses; positions define column order.
CREATE TABLE project_status (
    status_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES project(project_id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    position INTEGER NOT NULL CHECK (position >= 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_project_status_name UNIQUE (project_id, name),
    CONSTRAINT uq_project_status_id_project UNIQUE (status_id, project_id)
);

CREATE TABLE task (
    task_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES project(project_id),
    status_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    start_date DATE,
    due_date DATE,
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM'
        CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT')),
    created_by BIGINT NOT NULL REFERENCES member(member_id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_task_dates CHECK (
        start_date IS NULL OR due_date IS NULL OR due_date >= start_date
    ),
    CONSTRAINT fk_task_project_status FOREIGN KEY (status_id, project_id)
        REFERENCES project_status(status_id, project_id)
);

CREATE TABLE comment (
    comment_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    task_id BIGINT NOT NULL REFERENCES task(task_id) ON DELETE CASCADE,
    member_id BIGINT NOT NULL REFERENCES member(member_id),
    comment TEXT NOT NULL CHECK (length(btrim(comment)) > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE space_member (
    space_id BIGINT NOT NULL REFERENCES space(space_id) ON DELETE CASCADE,
    member_id BIGINT NOT NULL REFERENCES member(member_id),
    role VARCHAR(30) NOT NULL DEFAULT 'SPACE_MEMBER'
        CHECK (role IN ('SPACE_OWNER', 'SPACE_MEMBER')),
    joined_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (space_id, member_id)
);

CREATE TABLE project_member (
    project_id BIGINT NOT NULL REFERENCES project(project_id) ON DELETE CASCADE,
    member_id BIGINT NOT NULL REFERENCES member(member_id),
    role VARCHAR(30) NOT NULL DEFAULT 'PROJECT_MEMBER'
        CHECK (role IN ('PROJECT_MANAGER', 'PROJECT_MEMBER')),
    joined_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (project_id, member_id)
);

CREATE TABLE task_assignee (
    task_id BIGINT NOT NULL REFERENCES task(task_id) ON DELETE CASCADE,
    member_id BIGINT NOT NULL REFERENCES member(member_id),
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (task_id, member_id)
);

-- Snapshot the status label so renames do not erase historical meaning.
-- History is preserved if a project status is removed.
CREATE TABLE task_status_history (
    history_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    task_id BIGINT NOT NULL REFERENCES task(task_id) ON DELETE CASCADE,
    status_id BIGINT,
    status_name VARCHAR(100) NOT NULL,
    updated_by BIGINT NOT NULL REFERENCES member(member_id),
    updated_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_history_status FOREIGN KEY (status_id)
        REFERENCES project_status(status_id) ON DELETE SET NULL
);

-- Every new project receives default columns. Application service logic
-- should insert these within the same transaction as project creation.
-- Suggested values: (To Do, 0), (In Progress, 1), (Done, 2).
