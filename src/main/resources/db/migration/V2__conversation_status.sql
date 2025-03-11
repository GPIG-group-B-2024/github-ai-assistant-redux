CREATE TYPE conversation_status AS ENUM('IN_PROGRESS', 'COMPLETED', 'FAILED');

ALTER TABLE llm_conversation ADD COLUMN status conversation_status DEFAULT 'COMPLETED';