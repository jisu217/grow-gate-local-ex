CREATE DATABASE IF NOT EXISTS growgate
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE growgate;

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS companies (
  id VARCHAR(36) NOT NULL,
  name VARCHAR(200) NOT NULL,
  business_number VARCHAR(20) NULL,
  industry VARCHAR(100) NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_companies_business_number (business_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS users (
  id VARCHAR(36) NOT NULL,
  email VARCHAR(255) NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  name VARCHAR(100) NOT NULL,
  user_type VARCHAR(20) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_users_email (email),
  CONSTRAINT chk_users_user_type CHECK (user_type IN ('personal', 'company'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS memberships (
  id VARCHAR(36) NOT NULL,
  user_id VARCHAR(36) NOT NULL,
  company_id VARCHAR(36) NOT NULL,
  role VARCHAR(20) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_memberships_user_company (user_id, company_id),
  KEY idx_memberships_company_id (company_id),
  CONSTRAINT fk_memberships_user FOREIGN KEY (user_id) REFERENCES users (id),
  CONSTRAINT fk_memberships_company FOREIGN KEY (company_id) REFERENCES companies (id),
  CONSTRAINT chk_memberships_role CHECK (role IN ('owner', 'admin', 'editor', 'viewer'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS products (
  id VARCHAR(36) NOT NULL,
  owner_user_id VARCHAR(36) NOT NULL,
  company_id VARCHAR(36) NULL,
  name VARCHAR(200) NOT NULL,
  category VARCHAR(100) NULL,
  description TEXT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  KEY idx_products_owner_user_id (owner_user_id),
  KEY idx_products_company_id (company_id),
  CONSTRAINT fk_products_owner FOREIGN KEY (owner_user_id) REFERENCES users (id),
  CONSTRAINT fk_products_company FOREIGN KEY (company_id) REFERENCES companies (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS product_knowledge (
  id VARCHAR(36) NOT NULL,
  product_id VARCHAR(36) NOT NULL,
  trust_grade VARCHAR(10) NULL,
  source_type VARCHAR(100) NULL,
  notes TEXT NULL,
  created_by VARCHAR(36) NULL,
  updated_by VARCHAR(36) NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_product_knowledge_product_id (product_id),
  KEY idx_product_knowledge_created_by (created_by),
  KEY idx_product_knowledge_updated_by (updated_by),
  CONSTRAINT fk_product_knowledge_product FOREIGN KEY (product_id) REFERENCES products (id),
  CONSTRAINT fk_product_knowledge_created_by FOREIGN KEY (created_by) REFERENCES users (id),
  CONSTRAINT fk_product_knowledge_updated_by FOREIGN KEY (updated_by) REFERENCES users (id),
  CONSTRAINT chk_product_knowledge_trust_grade CHECK (trust_grade IN ('high', 'medium', 'low') OR trust_grade IS NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS certifications (
  id VARCHAR(36) NOT NULL,
  product_id VARCHAR(36) NOT NULL,
  name VARCHAR(200) NOT NULL,
  issued_at DATE NULL,
  expires_at DATE NULL,
  renewal_cycle INT NULL,
  created_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  KEY idx_certifications_product_id (product_id),
  KEY idx_certifications_expires_at (expires_at),
  CONSTRAINT fk_certifications_product FOREIGN KEY (product_id) REFERENCES products (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS fto (
  id VARCHAR(36) NOT NULL,
  product_id VARCHAR(36) NOT NULL,
  target_country VARCHAR(100) NOT NULL,
  risk_level VARCHAR(10) NULL,
  summary TEXT NULL,
  expires_at DATE NULL,
  created_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  KEY idx_fto_product_id (product_id),
  KEY idx_fto_target_country (target_country),
  CONSTRAINT fk_fto_product FOREIGN KEY (product_id) REFERENCES products (id),
  CONSTRAINT chk_fto_risk_level CHECK (risk_level IN ('high', 'medium', 'low') OR risk_level IS NULL)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS buyer_requirements (
  id VARCHAR(36) NOT NULL,
  product_id VARCHAR(36) NOT NULL,
  buyer_name VARCHAR(200) NOT NULL,
  country VARCHAR(100) NULL,
  requirements TEXT NOT NULL,
  created_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  KEY idx_buyer_requirements_product_id (product_id),
  KEY idx_buyer_requirements_country (country),
  CONSTRAINT fk_buyer_requirements_product FOREIGN KEY (product_id) REFERENCES products (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS knowledge_sources (
  id VARCHAR(36) NOT NULL,
  knowledge_id VARCHAR(36) NOT NULL,
  url TEXT NULL,
  document_name VARCHAR(200) NULL,
  retrieved_at DATE NULL,
  PRIMARY KEY (id),
  KEY idx_knowledge_sources_knowledge_id (knowledge_id),
  CONSTRAINT fk_knowledge_sources_knowledge FOREIGN KEY (knowledge_id) REFERENCES product_knowledge (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS audit_logs (
  id VARCHAR(36) NOT NULL,
  product_id VARCHAR(36) NOT NULL,
  changed_by VARCHAR(36) NOT NULL,
  change_type VARCHAR(20) NOT NULL,
  changed_fields TEXT NULL,
  previous_value TEXT NULL,
  new_value TEXT NULL,
  reason VARCHAR(500) NULL,
  changed_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  KEY idx_audit_logs_product_id (product_id),
  KEY idx_audit_logs_changed_by (changed_by),
  KEY idx_audit_logs_changed_at (changed_at),
  CONSTRAINT fk_audit_logs_product FOREIGN KEY (product_id) REFERENCES products (id),
  CONSTRAINT fk_audit_logs_changed_by FOREIGN KEY (changed_by) REFERENCES users (id),
  CONSTRAINT chk_audit_logs_change_type CHECK (change_type IN ('create', 'update', 'delete'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS diagnosis_runs (
  id VARCHAR(36) NOT NULL,
  user_id VARCHAR(36) NOT NULL,
  company_id VARCHAR(36) NULL,
  product_id VARCHAR(36) NULL,
  input_data TEXT NULL,
  knowledge_snapshot TEXT NULL,
  result TEXT NULL,
  status VARCHAR(20) NOT NULL,
  target_country VARCHAR(100) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  KEY idx_diagnosis_runs_user_id (user_id),
  KEY idx_diagnosis_runs_company_id (company_id),
  KEY idx_diagnosis_runs_product_id (product_id),
  KEY idx_diagnosis_runs_created_at (created_at),
  CONSTRAINT fk_diagnosis_runs_user FOREIGN KEY (user_id) REFERENCES users (id),
  CONSTRAINT fk_diagnosis_runs_company FOREIGN KEY (company_id) REFERENCES companies (id),
  CONSTRAINT fk_diagnosis_runs_product FOREIGN KEY (product_id) REFERENCES products (id),
  CONSTRAINT chk_diagnosis_runs_status CHECK (status IN ('pending', 'running', 'done', 'failed'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS = 1;
