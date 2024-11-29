CREATE TABLE workspace
(
    id          UUID NOT NULL PRIMARY KEY,
    name        TEXT NOT NULL,
    description TEXT
);


CREATE TABLE github_repository
(
    id           UUID NOT NULL PRIMARY KEY,
    url          TEXT NOT NULL,
    full_name    TEXT NOT NULL,
    workspace_id UUID REFERENCES workspace (id)
);



CREATE TYPE member_type AS ENUM ('CONTAINER', 'COMPONENT', 'PERSON', 'SOFTWARE_SYSTEM');

CREATE TABLE member
(
    id           UUID        NOT NULL PRIMARY KEY,
    workspace_id UUID        NOT NULL REFERENCES workspace (id),
    type         member_type NOT NULL,
    name         TEXT        NOT NULL,
    description  TEXT,
    parent       UUID DEFAULT NULL REFERENCES member (id)
);

CREATE TABLE relationship
(
    start_member UUID NOT NULL REFERENCES member (id),
    end_member   UUID NOT NULL REFERENCES member (id),
    workspace_id UUID NOT NULL REFERENCES workspace (id),
    description  TEXT,
    PRIMARY KEY (start_member, end_member)
);