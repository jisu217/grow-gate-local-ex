USE growgate;

CREATE TABLE IF NOT EXISTS user_consent_records (
  id VARCHAR(36) NOT NULL,
  user_id VARCHAR(36) NOT NULL,
  policy_version VARCHAR(40) NOT NULL,
  terms_accepted BOOLEAN NOT NULL DEFAULT FALSE,
  privacy_accepted BOOLEAN NOT NULL DEFAULT FALSE,
  marketing_accepted BOOLEAN NOT NULL DEFAULT FALSE,
  accepted_at DATETIME(6) NOT NULL,
  withdrawn_at DATETIME(6) NULL,
  source VARCHAR(40) NOT NULL DEFAULT 'signup',
  PRIMARY KEY (id),
  KEY idx_user_consent_user_accepted (user_id, accepted_at),
  CONSTRAINT fk_user_consent_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
