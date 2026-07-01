CREATE TABLE IF NOT EXISTS public_data_collection_runs (
    id VARCHAR(36) PRIMARY KEY,
    diagnosis_id VARCHAR(36),
    requested_by VARCHAR(36),
    hs_code VARCHAR(20),
    target_country VARCHAR(40),
    product_name VARCHAR(160),
    status VARCHAR(20) NOT NULL,
    provider_count INTEGER NOT NULL DEFAULT 0,
    success_count INTEGER NOT NULL DEFAULT 0,
    fallback_count INTEGER NOT NULL DEFAULT 0,
    cache_hit_count INTEGER NOT NULL DEFAULT 0,
    error_summary TEXT,
    started_at TIMESTAMP NOT NULL,
    finished_at TIMESTAMP,
    CONSTRAINT fk_public_data_runs_user FOREIGN KEY (requested_by) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS public_data_records (
    id VARCHAR(36) PRIMARY KEY,
    run_id VARCHAR(36),
    provider VARCHAR(40) NOT NULL,
    endpoint VARCHAR(500),
    request_key VARCHAR(240) NOT NULL,
    hs_code VARCHAR(20),
    target_country VARCHAR(40),
    matched_key VARCHAR(500),
    used_for VARCHAR(60),
    raw_payload TEXT,
    normalized_payload TEXT,
    score_contribution INTEGER,
    confidence DOUBLE PRECISION,
    source_type VARCHAR(20),
    fallback_used BOOLEAN NOT NULL DEFAULT FALSE,
    cache_hit BOOLEAN NOT NULL DEFAULT FALSE,
    error_message TEXT,
    fetched_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP,
    CONSTRAINT fk_public_data_records_run FOREIGN KEY (run_id) REFERENCES public_data_collection_runs(id)
);

CREATE INDEX IF NOT EXISTS idx_public_data_runs_requested_by_started
    ON public_data_collection_runs (requested_by, started_at DESC);

CREATE INDEX IF NOT EXISTS idx_public_data_records_scope_fetched
    ON public_data_records (run_id, hs_code, target_country, fetched_at DESC);

CREATE INDEX IF NOT EXISTS idx_public_data_records_cache
    ON public_data_records (provider, request_key, expires_at DESC);

CREATE TABLE IF NOT EXISTS revoked_tokens (
    token_hash VARCHAR(64) PRIMARY KEY,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_revoked_tokens_expires_at
    ON revoked_tokens (expires_at);
