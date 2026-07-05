USE growgate;
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Judge-facing proof tables. These tables separate actual live/cache API proof
-- from fallback, crawl, heuristic, and synthetic seed records.
CREATE TABLE IF NOT EXISTS public_api_providers (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  provider_code VARCHAR(80) NOT NULL UNIQUE,
  agency_name VARCHAR(160) NOT NULL,
  dataset_name VARCHAR(240) NOT NULL,
  operation_url VARCHAR(1000) NOT NULL,
  service_url VARCHAR(1000),
  auth_env_key VARCHAR(160),
  auth_type VARCHAR(40) NOT NULL DEFAULT 'SERVICE_KEY',
  data_format VARCHAR(40),
  update_cycle VARCHAR(80),
  approval_type VARCHAR(120),
  is_motie_related BOOLEAN NOT NULL DEFAULT FALSE,
  is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
  expected_schema_version VARCHAR(80) NOT NULL DEFAULT 'v1',
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS public_api_proof_runs (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  proof_run_id VARCHAR(80) NOT NULL UNIQUE,
  requested_by VARCHAR(36),
  trigger_type VARCHAR(40) NOT NULL,
  git_commit VARCHAR(80),
  app_version VARCHAR(80),
  server_profile VARCHAR(80),
  started_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  completed_at DATETIME(6) NULL,
  total_provider_count INT NOT NULL DEFAULT 0,
  live_provider_count INT NOT NULL DEFAULT 0,
  cache_provider_count INT NOT NULL DEFAULT 0,
  fallback_provider_count INT NOT NULL DEFAULT 0,
  motie_related_live_count INT NOT NULL DEFAULT 0,
  operation_endpoint_live_count INT NOT NULL DEFAULT 0,
  api_key_configured_live_count INT NOT NULL DEFAULT 0,
  schema_pass_count INT NOT NULL DEFAULT 0,
  schema_fail_count INT NOT NULL DEFAULT 0,
  proof_status VARCHAR(40) NOT NULL DEFAULT 'RUNNING',
  fail_reasons JSON,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  KEY idx_proof_requested_by (requested_by),
  KEY idx_proof_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS public_api_raw_responses (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  proof_run_id VARCHAR(80) NOT NULL,
  provider_code VARCHAR(80) NOT NULL,
  request_url_redacted VARCHAR(1200) NOT NULL,
  request_params_hash VARCHAR(96),
  api_key_configured BOOLEAN NOT NULL DEFAULT FALSE,
  operation_endpoint BOOLEAN NOT NULL DEFAULT FALSE,
  http_status INT,
  content_type VARCHAR(120),
  source_type VARCHAR(40) NOT NULL,
  source_quality_status VARCHAR(40) NOT NULL DEFAULT 'HEURISTIC_ONLY',
  record_count INT NOT NULL DEFAULT 0,
  latency_ms BIGINT,
  raw_response_sha256 VARCHAR(96),
  raw_response_path VARCHAR(1000),
  schema_name VARCHAR(120),
  schema_validation_status VARCHAR(40),
  schema_validation_errors JSON,
  fallback_reason VARCHAR(1000),
  fetched_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  INDEX idx_proof_provider (proof_run_id, provider_code),
  INDEX idx_source_type (source_type),
  INDEX idx_hash (raw_response_sha256)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS public_api_normalized_records (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  proof_run_id VARCHAR(80) NOT NULL,
  provider_code VARCHAR(80) NOT NULL,
  external_record_id VARCHAR(240),
  normalized_record_type VARCHAR(80) NOT NULL,
  title VARCHAR(500),
  country_code VARCHAR(20),
  hs_code VARCHAR(20),
  product_keyword VARCHAR(240),
  published_at DATETIME(6) NULL,
  source_url VARCHAR(1000),
  normalized_payload JSON NOT NULL,
  raw_response_sha256 VARCHAR(96),
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  INDEX idx_provider_record_type (provider_code, normalized_record_type),
  INDEX idx_query_keys (country_code, hs_code, product_keyword)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS public_data_score_contributions (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  diagnosis_id VARCHAR(36),
  proof_run_id VARCHAR(80) NOT NULL,
  provider_code VARCHAR(80) NOT NULL,
  dimension VARCHAR(80) NOT NULL,
  base_score DECIMAL(8,3) NOT NULL DEFAULT 0,
  public_data_delta DECIMAL(8,3) NOT NULL DEFAULT 0,
  guarded_delta DECIMAL(8,3) NOT NULL DEFAULT 0,
  final_score DECIMAL(8,3) NOT NULL DEFAULT 0,
  source_type VARCHAR(40) NOT NULL,
  clamp_reason VARCHAR(500),
  evidence_record_ids JSON,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  INDEX idx_score_contribution_proof (proof_run_id),
  INDEX idx_score_contribution_diagnosis (diagnosis_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ml_model_registry (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  model_name VARCHAR(120) NOT NULL,
  model_version VARCHAR(80) NOT NULL,
  task_type VARCHAR(80) NOT NULL,
  artifact_path VARCHAR(1000) NOT NULL,
  metrics_json JSON NOT NULL,
  feature_schema_json JSON NOT NULL,
  training_manifest_json JSON NOT NULL,
  is_active BOOLEAN NOT NULL DEFAULT FALSE,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  UNIQUE KEY uk_model_version (model_name, model_version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ml_shap_explanations (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  prediction_run_id BIGINT,
  diagnosis_id VARCHAR(36) NOT NULL,
  model_name VARCHAR(120) NOT NULL,
  model_version VARCHAR(80) NOT NULL,
  base_value DECIMAL(12,6),
  predicted_value DECIMAL(12,6),
  top_features_json JSON NOT NULL,
  explanation_agent_summary TEXT,
  explanation_limitations JSON,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  INDEX idx_ml_shap_diagnosis (diagnosis_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS synthetic_validation_personas (
  id VARCHAR(40) NOT NULL PRIMARY KEY,
  company_scenario VARCHAR(120) NOT NULL,
  role_name VARCHAR(120) NOT NULL,
  core_problem TEXT NOT NULL,
  validation_focus TEXT NOT NULL,
  synthetic_disclaimer BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS synthetic_validation_sessions (
  id VARCHAR(40) NOT NULL PRIMARY KEY,
  persona_id VARCHAR(40) NOT NULL,
  company_scenario VARCHAR(120) NOT NULL,
  task_id VARCHAR(40) NOT NULL,
  scenario_id VARCHAR(80) NOT NULL,
  baseline_manual_time_min INT NOT NULL,
  growgate_time_min INT NOT NULL,
  task_success BOOLEAN NOT NULL,
  confidence_before INT NOT NULL,
  confidence_after INT NOT NULL,
  source_trust_score INT NOT NULL,
  report_usefulness INT NOT NULL,
  willingness_to_pay_monthly_krw INT NOT NULL,
  top_objection TEXT,
  synthetic_disclaimer BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  KEY idx_validation_persona (persona_id),
  KEY idx_validation_scenario (scenario_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS synthetic_validation_findings (
  id VARCHAR(40) NOT NULL PRIMARY KEY,
  persona_id VARCHAR(40) NOT NULL,
  severity VARCHAR(40) NOT NULL,
  finding TEXT NOT NULL,
  recommended_fix TEXT NOT NULL,
  synthetic_disclaimer BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  KEY idx_validation_finding_persona (persona_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
