CREATE TABLE llm_conversation
(
    id      UUID NOT NULL PRIMARY KEY,
    repo_id UUID NOT NULL REFERENCES github_repository (id),
    issue_id INTEGER NOT NULL,
    created_at timestamptz
);

CREATE TYPE llm_message_role AS ENUM ('SYSTEM', 'USER', 'ASSISTANT');

CREATE TABLE llm_message
(
    id         UUID             NOT NULL PRIMARY KEY,
    role       llm_message_role NOT NULL,
    content    TEXT,
    created_at timestamptz
);

CREATE TABLE conversation_message
(
    conversation_id UUID NOT NULL REFERENCES llm_conversation (id),
    message_id      UUID NOT NULL REFERENCES llm_message (id),
    PRIMARY KEY (conversation_id, message_id)
)