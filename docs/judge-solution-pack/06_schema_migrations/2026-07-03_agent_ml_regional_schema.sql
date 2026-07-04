USE growgate;
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Optional schema extension for judge-facing evidence. Safe to run after the base schema.
CREATE TABLE IF NOT EXISTS synthetic_data_registry (
  id VARCHAR(36) NOT NULL PRIMARY KEY,
  entity_type VARCHAR(40) NOT NULL,
  entity_id VARCHAR(36) NOT NULL,
  synthetic_level VARCHAR(40) NOT NULL DEFAULT 'synthetic_demo_not_real_customer',
  source_pack VARCHAR(120) NOT NULL,
  generated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  notes TEXT NULL,
  UNIQUE KEY uk_synthetic_registry_entity (entity_type, entity_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS regional_industry_profiles (
  id VARCHAR(36) NOT NULL PRIMARY KEY,
  region_code VARCHAR(60) NOT NULL,
  region_name VARCHAR(100) NOT NULL,
  industry VARCHAR(120) NOT NULL,
  cluster_keywords TEXT NULL,
  support_orgs TEXT NULL,
  public_source_name VARCHAR(200) NULL,
  source_period VARCHAR(80) NULL,
  refresh_cycle_hours INT NOT NULL DEFAULT 720,
  synthetic BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  KEY idx_regional_profile_region_industry (region_code, industry)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS agent_trace_events (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  agent_run_id VARCHAR(64) NOT NULL,
  diagnosis_id VARCHAR(36) NOT NULL,
  step_no INT NOT NULL,
  agent_name VARCHAR(80) NOT NULL,
  tool_name VARCHAR(160) NOT NULL,
  input_hash VARCHAR(96),
  output_schema VARCHAR(120),
  output_summary VARCHAR(1000),
  source_type VARCHAR(32) NOT NULL DEFAULT 'agent',
  provider VARCHAR(80),
  endpoint VARCHAR(500),
  latency_ms BIGINT,
  status VARCHAR(32) NOT NULL,
  retry_count INT NOT NULL DEFAULT 0,
  fallback_reason VARCHAR(1000),
  evidence_count INT NOT NULL DEFAULT 0,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  KEY idx_agent_trace_diagnosis (diagnosis_id),
  KEY idx_agent_trace_run_step (agent_run_id, step_no),
  KEY idx_agent_trace_agent_status (agent_name, status),
  UNIQUE KEY uk_agent_trace_run_step (agent_run_id, step_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS ml_prediction_runs (
  id VARCHAR(36) NOT NULL PRIMARY KEY,
  diagnosis_id VARCHAR(36) NOT NULL,
  model_name VARCHAR(120) NOT NULL,
  model_version VARCHAR(60) NOT NULL,
  task_type VARCHAR(40) NOT NULL,
  predicted_label VARCHAR(40) NULL,
  predicted_value DOUBLE NULL,
  probability DOUBLE NULL,
  feature_payload JSON NULL,
  shap_summary JSON NULL,
  synthetic_training BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  KEY idx_ml_prediction_diagnosis (diagnosis_id),
  CONSTRAINT chk_ml_task_type CHECK (task_type IN ('gate_classification','delay_regression','quote_hours_regression','support_match_classification'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS company_validation_interviews (
  id VARCHAR(36) NOT NULL PRIMARY KEY,
  company_id VARCHAR(36) NULL,
  interview_type VARCHAR(40) NOT NULL,
  participant_role VARCHAR(80) NOT NULL,
  consent_recorded BOOLEAN NOT NULL DEFAULT FALSE,
  before_minutes INT NULL,
  after_minutes INT NULL,
  top_pain_points TEXT NULL,
  adoption_signal VARCHAR(40) NULL,
  anonymized_quote TEXT NULL,
  synthetic BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  KEY idx_validation_company (company_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
