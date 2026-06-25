PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS companies (
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  business_number TEXT UNIQUE,
  industry TEXT,
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS users (
  id TEXT PRIMARY KEY,
  email TEXT NOT NULL UNIQUE,
  password_hash TEXT NOT NULL,
  name TEXT NOT NULL,
  user_type TEXT NOT NULL CHECK (user_type IN ('personal', 'company')),
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS memberships (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL,
  company_id TEXT NOT NULL,
  role TEXT NOT NULL CHECK (role IN ('owner', 'admin', 'editor', 'viewer')),
  created_at TEXT NOT NULL,
  UNIQUE (user_id, company_id),
  FOREIGN KEY (user_id) REFERENCES users (id),
  FOREIGN KEY (company_id) REFERENCES companies (id)
);

CREATE TABLE IF NOT EXISTS products (
  id TEXT PRIMARY KEY,
  owner_user_id TEXT NOT NULL,
  company_id TEXT,
  name TEXT NOT NULL,
  category TEXT,
  hs_code TEXT,
  target_country TEXT,
  description TEXT,
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL,
  FOREIGN KEY (owner_user_id) REFERENCES users (id),
  FOREIGN KEY (company_id) REFERENCES companies (id)
);

CREATE TABLE IF NOT EXISTS product_knowledge (
  id TEXT PRIMARY KEY,
  product_id TEXT NOT NULL UNIQUE,
  trust_grade TEXT CHECK (trust_grade IN ('high', 'medium', 'low') OR trust_grade IS NULL),
  source_type TEXT,
  notes TEXT,
  created_by TEXT,
  updated_by TEXT,
  created_at TEXT NOT NULL,
  updated_at TEXT NOT NULL,
  FOREIGN KEY (product_id) REFERENCES products (id),
  FOREIGN KEY (created_by) REFERENCES users (id),
  FOREIGN KEY (updated_by) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS certifications (
  id TEXT PRIMARY KEY,
  product_id TEXT NOT NULL,
  name TEXT NOT NULL,
  issued_at TEXT,
  expires_at TEXT,
  renewal_cycle INTEGER,
  created_at TEXT NOT NULL,
  FOREIGN KEY (product_id) REFERENCES products (id)
);

CREATE TABLE IF NOT EXISTS fto (
  id TEXT PRIMARY KEY,
  product_id TEXT NOT NULL,
  target_country TEXT NOT NULL,
  risk_level TEXT CHECK (risk_level IN ('high', 'medium', 'low') OR risk_level IS NULL),
  summary TEXT,
  expires_at TEXT,
  created_at TEXT NOT NULL,
  FOREIGN KEY (product_id) REFERENCES products (id)
);

CREATE TABLE IF NOT EXISTS buyer_requirements (
  id TEXT PRIMARY KEY,
  product_id TEXT NOT NULL,
  buyer_name TEXT NOT NULL,
  country TEXT,
  requirements TEXT NOT NULL,
  created_at TEXT NOT NULL,
  FOREIGN KEY (product_id) REFERENCES products (id)
);

CREATE TABLE IF NOT EXISTS knowledge_sources (
  id TEXT PRIMARY KEY,
  knowledge_id TEXT NOT NULL,
  url TEXT,
  document_name TEXT,
  retrieved_at TEXT,
  FOREIGN KEY (knowledge_id) REFERENCES product_knowledge (id)
);

CREATE TABLE IF NOT EXISTS audit_logs (
  id TEXT PRIMARY KEY,
  product_id TEXT NOT NULL,
  changed_by TEXT NOT NULL,
  change_type TEXT NOT NULL CHECK (change_type IN ('create', 'update', 'delete')),
  changed_fields TEXT,
  previous_value TEXT,
  new_value TEXT,
  reason TEXT,
  changed_at TEXT NOT NULL,
  FOREIGN KEY (product_id) REFERENCES products (id),
  FOREIGN KEY (changed_by) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS diagnosis_runs (
  id TEXT PRIMARY KEY,
  user_id TEXT NOT NULL,
  company_id TEXT,
  product_id TEXT,
  input_data TEXT,
  knowledge_snapshot TEXT,
  result TEXT,
  status TEXT NOT NULL CHECK (status IN ('pending', 'running', 'done', 'failed')),
  target_country TEXT NOT NULL,
  created_at TEXT NOT NULL,
  FOREIGN KEY (user_id) REFERENCES users (id),
  FOREIGN KEY (company_id) REFERENCES companies (id),
  FOREIGN KEY (product_id) REFERENCES products (id)
);

CREATE INDEX IF NOT EXISTS idx_memberships_user_id ON memberships (user_id);
CREATE INDEX IF NOT EXISTS idx_memberships_company_id ON memberships (company_id);
CREATE INDEX IF NOT EXISTS idx_products_owner_user_id ON products (owner_user_id);
CREATE INDEX IF NOT EXISTS idx_products_company_id ON products (company_id);
CREATE INDEX IF NOT EXISTS idx_products_hs_code ON products (hs_code);
CREATE INDEX IF NOT EXISTS idx_products_target_country ON products (target_country);
CREATE INDEX IF NOT EXISTS idx_certifications_product_id ON certifications (product_id);
CREATE INDEX IF NOT EXISTS idx_fto_product_id ON fto (product_id);
CREATE INDEX IF NOT EXISTS idx_buyer_requirements_product_id ON buyer_requirements (product_id);
CREATE INDEX IF NOT EXISTS idx_knowledge_sources_knowledge_id ON knowledge_sources (knowledge_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_product_id ON audit_logs (product_id);
CREATE INDEX IF NOT EXISTS idx_diagnosis_runs_product_id ON diagnosis_runs (product_id);
CREATE INDEX IF NOT EXISTS idx_diagnosis_runs_created_at ON diagnosis_runs (created_at);
