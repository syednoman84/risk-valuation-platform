-- Flyway migration for PostgreSQL
-- Creates model_executions, model_execution_chunks, model_execution_errors


CREATE TABLE IF NOT EXISTS model_executions (
                                                id UUID PRIMARY KEY,
                                                model_id UUID NOT NULL,
                                                model_version INTEGER NOT NULL,
                                                position_file_id UUID NOT NULL,
                                                assumption_set_id UUID NOT NULL,
                                                status VARCHAR(32) NOT NULL,
    total_loans BIGINT,
    chunk_size INTEGER,
    total_chunks INTEGER,
    processed_loans BIGINT NOT NULL DEFAULT 0,
    succeeded_loans BIGINT NOT NULL DEFAULT 0,
    failed_loans BIGINT NOT NULL DEFAULT 0,
    options JSONB,
    error_summary TEXT,
    cancel_requested BOOLEAN NOT NULL DEFAULT FALSE,
    requested_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    started_at TIMESTAMP WITHOUT TIME ZONE,
    completed_at TIMESTAMP WITHOUT TIME ZONE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL
    );


CREATE INDEX IF NOT EXISTS idx_model_executions_status ON model_executions(status);
CREATE INDEX IF NOT EXISTS idx_model_executions_requested_at ON model_executions(requested_at);


CREATE TABLE IF NOT EXISTS model_execution_chunks (
                                                      id UUID PRIMARY KEY,
                                                      execution_id UUID NOT NULL REFERENCES model_executions(id) ON DELETE CASCADE,
    chunk_index INTEGER NOT NULL,
    start_offset BIGINT NOT NULL,
    end_offset BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    worker_id VARCHAR(128),
    idempotency_key VARCHAR(128) NOT NULL,
    payload_checksum VARCHAR(64),
    started_at TIMESTAMP WITHOUT TIME ZONE,
    completed_at TIMESTAMP WITHOUT TIME ZONE,
    processed_loans BIGINT DEFAULT 0,
    succeeded_loans BIGINT DEFAULT 0,
    failed_loans BIGINT DEFAULT 0,
    last_error TEXT,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL,
    CONSTRAINT uc_chunk_execution_chunkindex UNIQUE (execution_id, chunk_index),
    CONSTRAINT uc_chunk_idempotency_key UNIQUE (idempotency_key)
    );


CREATE INDEX IF NOT EXISTS idx_chunks_execution ON model_execution_chunks(execution_id);
CREATE INDEX IF NOT EXISTS idx_chunks_status ON model_execution_chunks(status);


CREATE TABLE IF NOT EXISTS model_execution_errors (
                                                      id UUID PRIMARY KEY,
                                                      execution_id UUID NOT NULL REFERENCES model_executions(id) ON DELETE CASCADE,
    chunk_id UUID REFERENCES model_execution_chunks(id) ON DELETE CASCADE,
    loan_id VARCHAR(64),
    error_code VARCHAR(64),
    message TEXT NOT NULL,
    raw_input JSONB,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
    );


CREATE INDEX IF NOT EXISTS idx_errors_execution ON model_execution_errors(execution_id);
CREATE INDEX IF NOT EXISTS idx_errors_chunk ON model_execution_errors(chunk_id);